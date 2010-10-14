package org.gudy.azureus2.platform.macosx;

/**
 * Imported from Azureus 4 source tree with some mods adapted for the plist generated by the installer 
 */

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.gudy.azureus2.core3.util.FileUtil;

public class 
PListEditor 
{	
	private String plistFile;
	
	
	public 
	PListEditor(
		String plistFile )
	
		throws IOException
	{
		this.plistFile = plistFile;
		
		File	file  = new File( plistFile );
		
		if ( !file.exists()){
			
			throw( new IOException( "plist file '" + file + "' doesn't exist" ));
		}
		
		if ( !file.canWrite()){
			
			throw( new IOException( "plist file '" + file + "' is read only" ));
		}
	}
	
	public void 
	setFileTypeExtensions(
		String[] extensions )
	
		throws IOException
	{
		StringBuffer value = new StringBuffer();
		StringBuffer find = new StringBuffer();
		find.append("(?s).*?<key>CFBundleDocumentTypes</key>\\s*<array>.*?<key>CFBundleTypeExtensions</key>\\s*<array>");
		for(int i = 0 ; i < extensions.length ; i++) {
			value.append("\n\t\t\t\t<string>");
			value.append(extensions[i]);
			value.append("</string>");
			
			find.append(".*?");
			find.append(extensions[i]);
		}
		value.append("\n\t\t\t");
		
		find.append(".*?</array>.*");
		String match = "(?s)(<key>CFBundleDocumentTypes</key>\\s*<array>.*?<key>CFBundleTypeExtensions</key>\\s*<array>)(.*?)(</array>)";
		
		setValue(find.toString(), match, value.toString());
	}
	
	public void 
	setSimpleStringValue(
		String key,
		String value)
	
		throws IOException
	{
		String find = "(?s).*?<key>" + key + "</key>\\s*" + "<string>" + value + "</string>.*";
		String match = "(?s)(<key>" + key + "</key>\\s*" + "<string>)(.*?)(</string>)";
		setValue(find, match, value);
	}
	
	/**
	 * If we fail to set the CFBundleURLSchemes it's because we didn't have in the plist in the first place -- 
	 * need to insert it here. 
	 */
	public void
	getPushyWithURLTypes(String [] types) throws IOException {
		StringBuffer content = new StringBuffer(getFileContent());
		int insertionIndex = content.indexOf("<key>CFBundleExecutable</key>");
		if( insertionIndex == -1 ) { 
			throw new IOException("Malformed PList -- can't find CFBundleExecutable");
		}
		
		StringBuilder entry = new StringBuilder();
		entry.append("<key>CFBundleURLTypes</key>\n"); 
		entry.append("<array>\n" );
		entry.append("	<dict>\n" ); 
		entry.append("		<key>CFBundleURLName</key>\n" ); 
		entry.append("		<string>magnet url</string>\n" ); 
		entry.append("		<key>CFBundleURLSchemes</key>\n" ); 
		entry.append("		<array>\n" );
		for( String type : types ) {
		entry.append("			<string>" + type + "</string>\n");
		}
		entry.append("		</array>\n" );
		entry.append("	</dict>\n" );
		entry.append("</array>\n" );
		
		content.insert(insertionIndex, entry );
		setFileContent(content.toString());
		touchFile();
	}
	
	public boolean
	setArrayValues(
		String key,
		String valueType,
		String[] values) 
	
		throws IOException
	{
		StringBuffer value = new StringBuffer();
		StringBuffer find = new StringBuffer();
		find.append("(?s).*?<key>" + key + "</key>\\s*" + "<array>");
		for(int i = 0 ; i < values.length ; i++) {
			find.append("\\s*<" + valueType + ">" + values[i] + "</" + valueType + ">");
			value.append("\n\t\t\t\t<" + valueType + ">");
			value.append(values[i]);
			value.append("</" + valueType + ">");
		}
		find.append("\\s*</array>.*");
		value.append("\n\t\t\t");
		
		String match = "(?s)(<key>" + key + "</key>\\s*<array>)(.*?)(</array>)";
		
		/**
		 * This can happen on firstrun since the OneSwarm plist doesn't have values like CFBundleURLSchemes, so add it here.
		 * Where? We can add this in right before  CFBundleExecutable, which we will have
		 */
		return setValue(find.toString(),match,value.toString());
	}
	
	private boolean 
	isValuePresent(
		String match )
	
		throws IOException
	{
		String fileContent = getFileContent();
		
		//System.out.println("Searching for:\n" + match);
		return fileContent.matches(match);
	}
	

	/**
	 * 
	 * @param find the regex expression to find if the value is already present
	 * @param match the regex expression that will match for the replace, it needs to capture 3 groups, the 2nd one being replaced by value
	 * @param value the value that replaces the 2nd match group
	 */
	private boolean 
	setValue(
		String find,
		String match,
		String value)
	
		throws IOException
	{
		String fileContent = getFileContent();
		
		if( !isValuePresent(find)) {
			//System.out.println("Changing " +plistFile);
			String oldContent = fileContent;
			fileContent = fileContent.replaceFirst(match, "$1"+value + "$3");
			setFileContent(fileContent);
			touchFile();
			
			return !oldContent.equals(fileContent);
		}
		
		return true;
	}
	
	private String 
	getFileContent()
		throws IOException
	{
		FileReader fr = null;
		
		try{
			fr = new FileReader(plistFile);
			//max 32KB
			int length = 32 * 1024;
			char[] buffer = new char[length];
			int offset = 0;
			int len = 0;
			
			while((len = fr.read(buffer,offset,length-offset)) > 0) {
				offset += len;
			}
			
			String result =  new String(buffer,0,offset);
			
			return result;
			
		} finally {
			if(fr != null) {
				fr.close();
			}
		}
		
		
		//return FileUtil.readFileAsString(new File(plistFile), 64*1024, "UTF-8" );
	}
	
	private void 
	setFileContent(
		String fileContent )
	
		throws IOException
	{
		File	file		= new File( plistFile );
		
		File	backup_file = new File( plistFile + ".bak" );
		
		if ( file.exists()){
			
			if ( !FileUtil.copyFile( file, backup_file )){
				
				throw( new IOException( "Failed to backup plist file prior to modification" ));
			}
		}
		
		boolean	ok = false;
		
		try{
			
			FileWriter fw = null;
			
			try{
				
				fw = new FileWriter(plistFile);
				fw.write(fileContent);
	
			} finally {
				
				if( fw != null ){
					
					fw.close();
					
					ok = true;
				}
			}
		}finally{
			if ( ok ){
				
				backup_file.delete();
				
			}else{
				
				if ( backup_file.exists()){
					
					File	bork_file = new File( plistFile + ".bad" );

					file.renameTo( bork_file );
					
					file.delete();
					
					backup_file.renameTo( file );
				}
			}
		}
	}
	
	public void 
	touchFile()
	{
		File	file  = new File( plistFile );
		for(int i = 0 ; i <= 2 ; i++) {
			if(file != null) {
				String command[] = new String[] { "touch", file.getAbsolutePath() };
				
				try{
					Runtime.getRuntime().exec(command);
					
				} catch(Exception e) {
					
					e.printStackTrace();
				}
			}
			file = file.getParentFile();
		}
	}
	
	public static void main(String args[]) {
		try{
			PListEditor editor = new PListEditor("/Applications/OneSwarm.app/Contents/Info.plist");
			editor.setFileTypeExtensions(new String[] {"torrent","tor","oneswarm"});
			editor.setSimpleStringValue("CFBundleName", "OneSwarm");
			editor.setSimpleStringValue("CFBundleTypeName", "OneSwarm Download");
			editor.setSimpleStringValue("CFBundleGetInfoString","OneSwarm");
			editor.setArrayValues("CFBundleURLSchemes", "string", new String[] {"magnet","dht"});
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}

}
