package org.molgenis.framework.ui.html;

public class JQueryDataTable extends Table
{

	public JQueryDataTable(String name)
	{
		this(name, null);
	}
	
	public JQueryDataTable(String name, String label)
	{
		super(name, label);
		this.setLabel(label);
		super.setDefaultCellStyle("");
		super.setHeaderCellStyle("");
	}
	
	@Override
	public String toHtml() {
		String result = super.toHtml();
		result += "<script>$('#"+getId()+"')" +
				".css('min-height','100px')" +
				".dataTable({" +
				"\n\"bPaginate\": false," +
				"\n\"bLengthChange\": true," +
				"\n\"bFilter\": false," +
				"\n\"bSort\": false," +
				"\n\"bInfo\": false," +
				"\n\"bJQueryUI\": true})" +
				"</script>";
		return result;
	}

}