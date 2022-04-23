package dk.halim.ePassportTerminal;

import dk.halim.Controllers.*;
import dk.halim.Models.*;
import dk.halim.Views.*;

import com.fazecast.jSerialComm.SerialPort;

import dk.halim.Models.ePassport;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;

public class MainApplication extends Application {

    final String appName = "e-Passport Terminal";

    @FXML
    ComboBox comboBoxPorts;
    private Service<Void> taskReadNFC;
    private Service<Void> taskResetNFC;

    ObservableList<String> portList;

    @FXML
    SerialPort portConnected;

    @Override
    public void start(Stage stage) throws FileNotFoundException {

        ePassport passport = new ePassport();

        stage.setTitle(appName);
        Scene scene = new Scene(new Group(), 1240, 600);

        comboBoxPorts = new ComboBox(portList);

        portConnected = null;

        portList = FXCollections.observableArrayList();

        String[] serialPortNames = GetPorts.listConnections();

        for (String name : serialPortNames) {
            System.out.println("\nDetected Port: ");
            System.out.println(name);
            portList.add(name);
        }

        comboBoxPorts.setItems(portList);

        GridPane grid1 = new GridPane();
        grid1.setVgap(4);
        grid1.setHgap(10);
        grid1.setPadding(new Insets(5, 5, 5, 5));

        Button btnConnect = new Button("Connect");
        Button btnDisconnect = new Button("Disconnect");

        grid1.add(new Label("Port "), 0, 0);
        grid1.add(comboBoxPorts, 1, 0);
        grid1.add(btnConnect, 2, 0);

        grid1.add(btnDisconnect, 2, 0);
        btnDisconnect.setVisible(false);

        grid1.add(new Label("Document Number "), 0, 1);
        TextField documentNumber = new TextField("A0467622");
        grid1.add(documentNumber, 1, 1, 2, 1);

        grid1.add(new Label("Date of Birth "), 0, 2);
        TextField dateOfBirth = new TextField("08/06/1992");
        grid1.add(dateOfBirth, 1, 2, 2, 1);

        grid1.add(new Label("Expiry Date "), 0, 3);
        TextField dateOfExpiry = new TextField("23/12/2015");
        grid1.add(dateOfExpiry, 1, 3, 2, 1);

        Button readEPassport = new Button("Read e-Passport");
        readEPassport.setMinWidth(130);
        readEPassport.setMaxWidth(130);
        grid1.add(readEPassport, 1, 4);

        Button runSimulation = new Button("Run Simulation");
        runSimulation.setMinWidth(130);
        runSimulation.setMaxWidth(130);
        grid1.add(runSimulation, 2, 4);

        Button resetArduino = new Button("Reset Reader");
        resetArduino.setMinWidth(130);
        resetArduino.setMaxWidth(130);
        grid1.add(resetArduino, 1, 5);

        Image image = new Image("File:");
        ImageView PassportPhoto = new ImageView();
        PassportPhoto.setFitWidth(200);
        PassportPhoto.setFitHeight(200);
        PassportPhoto.setPreserveRatio(true);
        PassportPhoto.fitHeightProperty();
        PassportPhoto.setImage(image);
        grid1.add(PassportPhoto, 1, 7);

        grid1.add(new Label("First Name "), 0, 8);
        Label MRZfirstName = new Label("");
        grid1.add(MRZfirstName, 1, 8, 2, 1);

        grid1.add(new Label("Last Name "), 0, 9);
        Label MRZlastName = new Label("");
        grid1.add(MRZlastName, 1, 9, 2, 1);

        grid1.add(new Label("Nationality "), 0, 10);
        Label MRZNationality = new Label("");
        grid1.add(MRZNationality, 1, 10, 2, 1);

        grid1.add(new Label("Sex "), 0, 11);
        Label MRZSex = new Label("");
        grid1.add(MRZSex, 1, 11, 2, 1);

        ProgressBar progressReading = new ProgressBar(0);
        progressReading.setMinWidth(130);
        progressReading.setVisible(false);
        grid1.add(progressReading, 1, 25, 3, 1);

        Button btnExit = new Button("Exit");
        btnExit.setMinWidth(130);
        btnExit.setMaxWidth(130);
        grid1.add(btnExit, 1, 26);

        GridPane grid2 = new GridPane();
        grid2.setVgap(4);
        grid2.setHgap(10);
        grid2.setPadding(new Insets(5, 5, 5, 5));

        TextArea currentStatus = new TextArea("");
        currentStatus.setFont(Font.font("Courier", FontWeight.LIGHT, 16));
        currentStatus.setPrefHeight(570);
        currentStatus.setPrefWidth(700);
        currentStatus.setWrapText(true);
        grid2.add(currentStatus, 1, 0, 4, 8);

        HBox hBox = new HBox();
        hBox.setSpacing(5.0);
        hBox.setPadding(new Insets(5, 5, 5, 5));
        hBox.getChildren().addAll(grid1, grid2);

        Group root = (Group) scene.getRoot();
        root.getChildren().add(hBox);
        stage.setScene(scene);
        stage.show();

        btnConnect.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                btnConnect.setVisible(false);
                btnDisconnect.setVisible(true);

                String port = (String) comboBoxPorts.getValue();
                portConnected = ArduinoPort.connectArduino(port, currentStatus);
            }
        });

        btnDisconnect.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                btnDisconnect.setVisible(false);
                btnConnect.setVisible(true);

                ArduinoPort.disconnectArduino(currentStatus);

                portConnected = null;
            }
        });

        runSimulation.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                RunSimulation.Run();
            }
        });

        readEPassport.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                // Clear the status text for new reading
                currentStatus.setText("");

                if (portConnected != null) {

                    taskReadNFC = new Service<Void>() {
                        @Override
                        protected Task<Void> createTask() {

                            return new Task<Void>() {
                                @Override
                                protected Void call() throws Exception {

                                    PerformNFC.read(passport,
                                            portConnected,
                                            currentStatus,
                                            progressReading,
                                            documentNumber.getText(),
                                            dateOfBirth.getText(),
                                            dateOfExpiry.getText());

                                    return null;
                                }

                                @Override
                                protected void succeeded() {
                                    super.succeeded();
                                    System.out.println("Reading completed.");
                                }
                            };
                        }
                    };

                    taskReadNFC.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                        @Override
                        public void handle(WorkerStateEvent event) {

                            UpdateMRZ UpdateMRZ = new UpdateMRZ();
                            UpdateMRZ.show(passport,
                                    MRZfirstName,
                                    MRZlastName,
                                    MRZNationality,
                                    MRZSex);

                            if (passport.getDG2().isDG2Valid()) {
                                byte[] getDG2Image = passport.getDG2().getImage();
                                if (getDG2Image != null) {
                                    Image img = new Image(new ByteArrayInputStream(getDG2Image));
                                    PassportPhoto.setImage(img);
                                }
                            }

                        }
                    });

                    taskReadNFC.restart();

                }

            }
        });

        resetArduino.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                taskReadNFC = new Service<Void>() {
                    @Override
                    protected Task<Void> createTask() {

                        return new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {

                                try {
                                    ArduinoReset.reset(portConnected, currentStatus);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                return null;
                            }

                        };
                    }
                };

                taskReadNFC.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent event) {

                    }
                });

                taskReadNFC.restart();
            }
        });

        btnExit.setOnAction((ActionEvent event) -> {
            Platform.exit();
        });

    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }

}