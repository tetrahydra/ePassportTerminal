package dk.halim.Models;

import org.bouncycastle.util.encoders.Hex;

import java.io.FileOutputStream;
import java.io.IOException;

public class EF_DG2 {

    String JP2 = "0000000C6A5020200D0A870A";
    String JPG = "FFD8";

    String imageData = null;

    String[] splitData;

    String finalImage;

    public void setImageData(String data) {
        if (data.length() > 0) {
            this.imageData = data;
        }
    }

    public String getImageData() {
        return this.imageData;
    }

    public boolean isDG2Valid() {
        if (getImageData() != null && getImageData().length() > 0) {
            return true;
        }

        return false;
    }

    public byte[] getImage() {

        if (getImageData().contains(this.JP2)) {
            this.splitData = getImageData().split(this.JP2);
            this.finalImage = this.JP2 + this.splitData[1];
        }

        if (getImageData().contains(this.JPG)) {
            this.splitData = getImageData().split(this.JPG);
            this.finalImage = this.JPG + this.splitData[1];
        }

        if (this.finalImage != null && this.finalImage.length() > 0) {
            return Hex.decode(this.finalImage);
        } else {
            return null;
        }

    }

    public void saveImage(String filePath) {
        byte[] imageData = getImage();

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(imageData, 0, imageData.length);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
