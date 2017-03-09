package utils;

import java.io.File;
import javax.swing.filechooser.FileFilter;

// filter for file selection is possible only with the extension .mp3 to FileChooser component
public class MP3PlayerFileFilter extends FileFilter {
    
    private String fileExtension;
    private String fileDescription;

    public MP3PlayerFileFilter(String fileExtension, String fileDescription) {
        this.fileExtension = fileExtension;
        this.fileDescription = fileDescription;
    }
  
    @Override
    public boolean accept(File file) {// Allow only the folders and files with the extension .mp3
        return file.isDirectory() || file.getAbsolutePath().endsWith(fileExtension);
    }   

    @Override
    public String getDescription() {// Description for .mp3 format
        return fileDescription+" (*."+fileExtension+")";
    }
}
