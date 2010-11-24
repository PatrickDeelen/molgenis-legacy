package org.molgenis.framework.db;

import java.util.List;

import org.molgenis.framework.db.jdbc.ColumnInfo.Type;
import org.molgenis.util.CsvReader;
import org.molgenis.util.CsvWriter;

import org.molgenis.util.Entity;

public interface Mapper<E extends Entity> {
	public Database getDatabase();
	
	public int add(List<E> entities) throws DatabaseException;

	// FIXME: can we merge the two add functions by wrapping list/reader into an
	// iterator of some kind?
	public E create();
	
	public int add(CsvReader reader, CsvWriter writer) throws DatabaseException;
	
	public int update(List<E> entities) throws DatabaseException;
	
	public int update(CsvReader reader) throws DatabaseException;
	
	public int remove(List<E> entities) throws DatabaseException;
	
	public int count(QueryRule ...rules) throws DatabaseException;

	public List<E> find(QueryRule ...rules) throws DatabaseException;

	public void find(CsvWriter writer, QueryRule[] rules) throws DatabaseException;
	
	public void find(CsvWriter writer, List<String> fieldsToExport, QueryRule[] rules) throws DatabaseException;

	public int remove(CsvReader reader) throws DatabaseException;

	public List<E> toList(CsvReader reader, int limit) throws Exception;

	public String getTableFieldName(String field);

	public Type getFieldType(String field);	
	
}