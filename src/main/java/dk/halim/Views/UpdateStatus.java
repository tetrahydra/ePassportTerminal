package dk.halim.Views;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class UpdateStatus {

    public static void append(TextArea currentStatus, String text) {
        Platform.runLater(() -> currentStatus.appendText(text + "\n"));
        Platform.runLater(() -> currentStatus.selectPositionCaret(currentStatus.getLength()));
        Platform.runLater(() -> currentStatus.deselect());
    }

}
