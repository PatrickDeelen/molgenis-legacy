package org.molgenis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.listener.Log4jListener;
import org.molgenis.MolgenisOptions.MapperImplementation;
import org.molgenis.generators.DataTypeGen;
import org.molgenis.generators.Generator;
import org.molgenis.generators.JpaDataTypeGen;
import org.molgenis.generators.R.RApiGen;
import org.molgenis.generators.R.REntityGen;
import org.molgenis.generators.R.RMatrixGen;
import org.molgenis.generators.csv.CsvExportGen;
import org.molgenis.generators.csv.CsvImportByIdGen;
import org.molgenis.generators.csv.CsvImportGen;
import org.molgenis.generators.csv.CsvReaderGen;
import org.molgenis.generators.db.InMemoryDatabaseGen;
import org.molgenis.generators.db.JDBCDatabaseGen;
import org.molgenis.generators.db.JDBCMetaDatabaseGen;
import org.molgenis.generators.db.JpaDatabaseGen;
import org.molgenis.generators.db.JpaMapperGen;
import org.molgenis.generators.db.MapperDecoratorGen;
import org.molgenis.generators.db.MapperSecurityDecoratorGen;
import org.molgenis.generators.db.MultiqueryMapperGen;
import org.molgenis.generators.db.PStatementMapperGen;
import org.molgenis.generators.db.PersistenceGen;
import org.molgenis.generators.db.ViewMapperGen;
import org.molgenis.generators.doc.DotDocGen;
import org.molgenis.generators.doc.DotDocMinimalGen;
import org.molgenis.generators.doc.EntityModelDocGen;
import org.molgenis.generators.doc.FileFormatDocGen;
import org.molgenis.generators.doc.ObjectModelDocGen;
import org.molgenis.generators.doc.TableDocGen;
import org.molgenis.generators.doc.TextUmlGen;
import org.molgenis.generators.excel.ExcelExportGen;
import org.molgenis.generators.excel.ExcelImportGen;
import org.molgenis.generators.excel.ExcelReaderGen;
import org.molgenis.generators.excel.ImportWizardExcelPrognosisGen;
import org.molgenis.generators.python.PythonDataTypeGen;
import org.molgenis.generators.server.MolgenisContextListenerGen;
import org.molgenis.generators.server.MolgenisResourceCopyGen;
import org.molgenis.generators.server.MolgenisServletContextGen;
import org.molgenis.generators.server.MolgenisServletGen;
import org.molgenis.generators.server.RdfApiGen;
import org.molgenis.generators.server.RestApiGen;
import org.molgenis.generators.server.SoapApiGen;
import org.molgenis.generators.sql.CountPerEntityGen;
import org.molgenis.generators.sql.CountPerTableGen;
import org.molgenis.generators.sql.DerbyCreateSubclassPerTableGen;
import org.molgenis.generators.sql.FillMetadataTablesGen;
import org.molgenis.generators.sql.HSqlCreateSubclassPerTableGen;
import org.molgenis.generators.sql.MySqlAlterSubclassPerTableGen;
import org.molgenis.generators.sql.MySqlCreateSubclassPerTableGen;
import org.molgenis.generators.sql.PSqlCreateSubclassPerTableGen;
import org.molgenis.generators.tests.TestCsvGen;
import org.molgenis.generators.tests.TestDataSetGen;
import org.molgenis.generators.tests.TestDatabaseGen;
import org.molgenis.generators.ui.FormScreenGen;
import org.molgenis.generators.ui.HtmlFormGen;
import org.molgenis.generators.ui.MenuScreenGen;
import org.molgenis.generators.ui.PluginScreenFTLTemplateGen;
import org.molgenis.generators.ui.PluginScreenGen;
import org.molgenis.generators.ui.PluginScreenJavaTemplateGen;
import org.molgenis.generators.ui.TreeScreenGen;
import org.molgenis.model.MolgenisModel;
import org.molgenis.model.elements.Model;
import org.molgenis.util.cmdline.CmdLineException;

/**
 * MOLGENIS generator. Run this to fire up all the generators. Optionally add
 * {@link org.molgenis.MolgenisOptions}
 * 
 * @author Morris Swertz
 */
public class Molgenis {

	public static void main(String[] args) {
		try {
			if (args.length != 1) {
				throw new Exception("You have to provide the molgenis.properties file as first argument. Now got: "
								+ Arrays.toString(args));
			}
			new Molgenis(args[0]).generate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected transient static final Logger logger = Logger.getLogger("MOLGENIS");
	MolgenisOptions options = null;
	Model model = null;
	List<Generator> generators = new ArrayList<Generator>();

	public Molgenis(String propertiesFile, Class<? extends Generator>... generatorsToUse) throws Exception {
		this(new MolgenisOptions(propertiesFile), generatorsToUse);
	}

	/**
	 * Construct a MOLGENIS generator
	 * 
	 * @param options
	 *            with generator settings
	 * @param generatorsToUse
	 *            optional list of generator classes to include
	 * @throws Exception
	 */
	public <E extends Generator> Molgenis(MolgenisOptions options,
			Class<? extends Generator>... generatorsToUse) throws Exception {
		this.options = options;

		Logger.getLogger("freemarker.cache").setLevel(Level.INFO);
		logger.info("\nMOLGENIS version " + org.molgenis.Version.convertToString());
		logger.info("working dir: " + System.getProperty("user.dir"));

		// clean options
		if (!options.output_src.endsWith("/")) {
			options.output_src = options.output_src + "/";
		}
		if (!options.output_python.endsWith("/")) {
			options.output_python = options.output_python + "/";
		}
		if (!options.output_hand.endsWith("/")) {
			options.output_hand = options.output_hand + "/";
		}

		// COPY resources
		generators.add(new MolgenisResourceCopyGen());

		// DOCUMENTATION
		if (options.generate_doc.equals("true")){
			generators.add(new TableDocGen());
			generators.add(new EntityModelDocGen());
			generators.add(new DotDocGen());
			generators.add(new FileFormatDocGen());
			generators.add(new DotDocMinimalGen());
			generators.add(new ObjectModelDocGen());
			generators.add(new TextUmlGen());
		}else{
			logger.info("Skipping documentation ....");
		}

		// TESTS
		if (options.generate_tests.equals("true")){
			generators.add(new TestDatabaseGen());
			generators.add(new TestCsvGen());
			generators.add(new TestDataSetGen());
		}else{
			logger.info("Skipping Tests ....");
		}
		// DATA
		// generators.add(new DataPListGen());
		// generators.add(new ViewTypeGen());
		generators.add(new InMemoryDatabaseGen());

		
		if(options.mapper_implementation.equals(MapperImplementation.JPA)) {
			 generators.add(new JpaDatabaseGen());
			 generators.add(new JpaDataTypeGen());
			 //generators.add(new JpaDataTypeListenerGen());
			 generators.add(new JpaMapperGen());
			 generators.add(new JDBCMetaDatabaseGen());
			 if(options.generate_persisitence) {
				 generators.add(new PersistenceGen());
			 }
		} else {
			// DATABASE
			// mysql.org
			generators.add(new ViewMapperGen());
			if (options.db_driver.equals("com.mysql.jdbc.Driver")) {
				generators.add(new MySqlCreateSubclassPerTableGen());
				generators.add(new MySqlAlterSubclassPerTableGen());
				// use multiquery optimization
				if (options.mapper_implementation
						.equals(MapperImplementation.MULTIQUERY)) {
					generators.add(new JDBCDatabaseGen());
					generators.add(new DataTypeGen());
					generators.add(new PythonDataTypeGen());
					generators.add(new MultiqueryMapperGen());
				} else if (options.mapper_implementation
						.equals(MapperImplementation.PREPARED_STATEMENT)) {
					generators.add(new JDBCDatabaseGen());
					generators.add(new DataTypeGen());
					generators.add(new PythonDataTypeGen());
					generators.add(new PStatementMapperGen());
				} 
			}
			// hsqldb.org
			else if (options.db_driver.equals("org.hsqldb.jdbcDriver")) {
				logger.info("HsqlDB generators ....");
				generators.add(new JDBCDatabaseGen());
				generators.add(new DataTypeGen());
				generators.add(new HSqlCreateSubclassPerTableGen());
				//generators.add(new MultiqueryMapperGen());
				generators.add(new PStatementMapperGen());
			}
			// postgresql
			else if (options.db_driver.equals("org.postgresql.Driver")) {
				generators.add(new PSqlCreateSubclassPerTableGen());
				generators.add(new PStatementMapperGen());
			}

			// h2database.com, branch of hsqldb?
			else if (options.db_driver.equals("org.h2.Driver")) {
				generators.add(new HSqlCreateSubclassPerTableGen());
				generators.add(new PStatementMapperGen());
			}
			// derby, not functional ignore.
			else if (options.db_driver
					.equals("org.apache.derby.jdbc.EmbeddedDriver")) {
				generators.add(new DerbyCreateSubclassPerTableGen());
			} else {
				logger.warn("Unknown database driver " + options.db_driver);
				// System.exit(-1);
			}

			// JDBC authorization
			if (!options.auth_loginclass.endsWith("SimpleLogin"))
			{
				generators.add(new MapperSecurityDecoratorGen());
			}

			// decorators
			generators.add(new MapperDecoratorGen());

			// test
			generators.add(new JDBCMetaDatabaseGen());
			// SQL
			generators.add(new CountPerEntityGen());
			generators.add(new CountPerTableGen());
			generators.add(new FillMetadataTablesGen());			
		}
		

		
		if (options.generate_Python.equals("true")){
			generators.add(new PythonDataTypeGen());
		}else{
			logger.info("Skipping Python interface ....");
		}
		// generators.add(new HsqlDbGen());

		// CSV
		if (options.generate_csv.equals("true")){
			generators.add(new CsvReaderGen());
			generators.add(new CsvImportByIdGen());
			generators.add(new CsvExportGen());
			generators.add(new CsvImportGen());
		}else{
			logger.info("Skipping CSV importers ....");
		}
		// generators.add(new CopyMemoryToDatabaseGen());
		// generators.add(new CsvReaderFactoryGen());

		// XML
		// generators.add(new XmlMapperGen());

		// R
		if (options.generate_R.equals("true")){
			generators.add(new REntityGen());
			generators.add(new RMatrixGen());
			generators.add(new RApiGen());
		}else{
			logger.info("Skipping R interface ....");
		}
		// HTML
		generators.add(new HtmlFormGen());

		// ServletContext
		generators.add(new MolgenisServletContextGen());
		generators.add(new MolgenisContextListenerGen());

		// SCREEN
		if (options.generate_MolgenisServlet.equals("true")){
			generators.add(new MolgenisServletGen());
		}else{
			logger.info("Skipping MolgenisServlet ....");
		}
		generators.add(new FormScreenGen());
		generators.add(new MenuScreenGen());
		generators.add(new TreeScreenGen());

		// SCREEN PLUGIN
		generators.add(new PluginScreenGen());
		generators.add(new PluginScreenFTLTemplateGen());
		generators.add(new PluginScreenJavaTemplateGen());

		// SERVER SETTINGS
		generators.add(new MolgenisServletContextGen());

		// SOAP
		generators.add(new SoapApiGen());
		generators.add(new RestApiGen());

		// Excel
		if (options.generate_ExcelImport.equals("true")){
			generators.add(new ExcelReaderGen());
			generators.add(new ExcelImportGen());
			generators.add(new ExcelExportGen());
			generators.add(new ImportWizardExcelPrognosisGen());
		}else{
			logger.info("Skipping Excel importer ....");
		}
		// RDF
		generators.add(new RdfApiGen());

		// FIXME add more generators
		// FIXME use configuration to add the generators

		// clean out generators
		List<Generator> use = new ArrayList<Generator>();
		if (generatorsToUse.length > 0) {
			for (Class<? extends Generator> c : generatorsToUse) {
				use.add(c.newInstance());
			}
			generators = use;
		}

		logger.debug("\nUsing generators:\n" + toString());

		// parsing model
		model = MolgenisModel.parse(options);
	}

	/**
	 * Apply all generators on the model
	 * 
	 * @param model
	 */
	public void generate() throws Exception {
		logger.info("generating ....");
		logger.info("\nUsing options:\n" + options.toString());

		File generatedFolder = new File(options.output_src);
		logger.info("removing previous generated folder " + generatedFolder);
		if (generatedFolder.exists()) {
			generatedFolder.delete();
		}

		for (final Generator g : generators) {

			g.generate(model, options);
		}
		logger.info("Generation completed at " + new Date());
	}

	/**
	 * Compile a generated molgenis.
	 * 
	 * Currently not implemented but is needed for batch generation. Not needed
	 * if you are generating inside an IDE such as eclipse.
	 * 
	 * @return true if build is succesfull
	 * @throws IOException
	 */
	@Deprecated
	public boolean compile() throws IOException {
		// reduce loggin
		Logger.getLogger("org.apache.tools.ant.UnknownElement").setLevel(Level.ERROR);
		Logger.getLogger("org.apache.tools.ant.Target").setLevel(Level.ERROR);

		// run the ant build script
		logger.info("<b>Compile ...</b>");

		File tempdir = new File(options.output_src);
		// File tempdir = (File) ses.getAttribute("workingdir");

		// copy the buildfile from sjabloon
		File buildFileSource = new File("sjabloon/build.xml");
		File buildFile = new File(tempdir.getPath() + "/build.xml");
		copyFile(buildFileSource, buildFile);

		// create a new ant project
		Project p = new Project();
		p.setUserProperty("ant.file", buildFile.getAbsolutePath());
		p.init();

		// execute the ant target
		ProjectHelper helper = ProjectHelper.getProjectHelper();
		p.addReference("ant.projectHelper", helper);
		p.addBuildListener(new Log4jListener());
		helper.parse(p, buildFile);

		p.setProperty("jdbc.driver", "mysql-connector-java-5.1.0-bin.jar");
		p.setProperty("main.class", "MolgenisOnMysqlServer");
		p.executeTarget("createjar");
		logger.info("compilation complete.");

		return true;
	}

	private static void copyFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	/**
	 * Parse the model using settings in options
	 * 
	 * @param options
	 * @return MOLGENIS model
	 * @throws Exception
	 */
//	private void parse() throws Exception {
//		MolgenisModel language = new MolgenisModel();
//		model = language.parse(options);
//		logger.debug("\nUsing metamodel:\n" + model);
//	}

	/**
	 * Load the generated SQL into the database.
	 * 
	 * Warning: this will overwrite any existing data in the database!.
	 * 
	 * @throws SQLException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws CmdLineException
	 */
	public void updateDb() throws SQLException, FileNotFoundException,
	IOException, CmdLineException {
		updateDb(false);
	}
	
	
	public void updateDb(boolean filldb) throws SQLException, FileNotFoundException,
			IOException, CmdLineException {
		
		boolean ask = false;

		// ask for confirmation that the database can be updated
		// TODO: Use or throw away! Make a decision.
		while (ask) {
			logger.info("Are you sure that you want overwrite database "
					+ options.db_uri
					+ "?\n All existing data will be overwritten. \nAnswer 'y' or 'n'.\n");
			String answer = "";
			int c;
			while ((c = System.in.read()) != 13) {
				answer += (char) c;
			}
			if (answer.trim().equals("y")) {
				ask = false;
			} else if (answer.equals("n")) {
				logger.info("MOLGENIS database update canceled.\n");
				return;
			} else {
				logger.info("You must answer 'y' or 'n'.");
			}
		}

		// start loading
		BasicDataSource data_src = new BasicDataSource();
		Connection conn = null;
		try {
			data_src = new BasicDataSource();
			data_src.setDriverClassName(options.db_driver);
			data_src.setUsername(options.db_user);
			data_src.setPassword(options.db_password);
			data_src.setUrl(options.db_uri);

			conn = data_src.getConnection();
			String create_tables_file = options.output_sql + File.separator
					+ "create_tables.sql";
			logger.debug("using file " + create_tables_file);
			// String create_tables_file = "generated" + File.separator + "sql"
			// + File.separator + "create_tables.sql";

			// READ THE FILE
			String create_tables_sql = "";
			try {
				BufferedReader in = new BufferedReader(new FileReader(create_tables_file));
				String line;
				while ((line = in.readLine()) != null) {
					create_tables_sql += line + "\n";
				}
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (filldb && StringUtils.isNotEmpty(this.options.getAuthLoginclass()))
			{
				String insert_metadata_file = options.output_sql + File.separator + "insert_metadata.sql";
				logger.debug("using file " + insert_metadata_file);

				// READ THE FILE
				try {
					BufferedReader in = new BufferedReader(new FileReader(insert_metadata_file));
					String line;
					while ((line = in.readLine()) != null) {
						create_tables_sql += line + "\n";
					}
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			Statement stmt = conn.createStatement();
			boolean error = false;
			logger.info("Updating database....");
			int i = 0;
			for (String command : create_tables_sql.split(";")) {
				if (command.trim().length() > 0) {
					try {
						stmt.executeUpdate(command + ";");
						if (i++ % 10 == 0) {
							logger.debug(".");
						}
					} catch (Exception e) {
						error = true;
						logger.error("\nERROR executing command: " + command
								+ ";\n" + e.getMessage());
					}

				}
			}

			if (error) {
				logger.debug("Errors occured. Make sure you provided sufficient rights! Inside mysql paste the following, assuming your database is called 'molgenis':"
						+ "\ncreate database molgenis; "
						+ "\ngrant all privileges on molgenis.* to molgenis@localhost "
						+ "identified by 'molgenis';"
						+ "\nflush privileges;"
						+ "\nuse molgenis;");
			}

			logger.info("MOLGENIS database updated succesfully");
		} catch (Exception e) {
			logger.error(e);

		} finally {
			conn.close();
		}

	}

	/**
	 * Report current settings of the generator.
	 */
	@Override
	public final String toString() {
		StringBuffer result = new StringBuffer();

		// get name, description and padding
		Map<String, String> map = new LinkedHashMap<String, String>();
		int padding = 0;
		for (Generator g : generators) {
			// get the name (without common path)
			String generatorName = null;
			if (g.getClass().getName().indexOf(this.getClass().getPackage().getName()) == 0) {
				generatorName = g.getClass().getName().substring(this.getClass().getPackage().getName().length() + 1);
			} else {
				generatorName = g.getClass().getName();
			}

			// calculate the padding
			padding = Math.max(padding, generatorName.length());

			// add to map
			map.put(generatorName, g.getDescription());
		}

		// print
		for (Map.Entry<String,String> entry : map.entrySet()) {
			// create padding
			String spaces = "";
			for (int i = entry.getKey().toString().length(); i < padding; i++) {
				spaces += " ";
			}
			result.append(entry.getKey() + spaces + " #" + entry.getValue()
					+ "\n");
		}
		return result.toString();
	}
}