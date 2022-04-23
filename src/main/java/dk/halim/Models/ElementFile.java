package dk.halim.Models;

import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ElementFile {

    String data = "";

    int fileSize = 0;

    boolean stop = false;

    public void setStop(boolean data){
        this.stop = data;
    }

    public boolean getStop(){
        return this.stop;
    }

    public void append(String data){
        if (data != null && data.length() > 0) {
            this.data = this.data + data;
        }
    }

    public void reset(){
        this.data = "";
    }

    public String getData() {
        return this.data;
    }

    public byte[] getDataByte() {
        if (getData().length() > 0) {
            return Hex.decode(getData());
        }

        return null;
    }

    public int getDataSize(){
        if (getData().length() > 0) {
            return Hex.decode(getData()).length;
        }
        return 0;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public int getFileSize(){
        return this.fileSize;
    }

    public void saveOutput(File filePath){
        byte[] final_data = getDataByte();

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(final_data, 0, final_data.length);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readFile(File filePath) throws IOException {
        File inputFile = new File(String.valueOf(filePath));
        byte[] data = new byte[(int) inputFile.length()];
        FileInputStream fis = new FileInputStream(inputFile);
        fis.read(data, 0, data.length);
        fis.close();
    }
}
