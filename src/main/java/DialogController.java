import com.jfoenix.controls.JFXProgressBar;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class DialogController implements Initializable {
    Task copyWorker;
    @FXML
    Label label;
    @FXML
    JFXProgressBar progressBar;
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try(ServerSocket serverSocket = new ServerSocket(25000)){


            copyWorker=createWorker("Softablitz-PS.pdf");
            progressBar.progressProperty().unbind();
            progressBar.progressProperty().bind(copyWorker.progressProperty());

            copyWorker.messageProperty().addListener(new ChangeListener<String>() {
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    label.setText(newValue);
                }
            });

            Thread t=new Thread(copyWorker);
            t.setDaemon(true);
            t.start();

        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public Task createWorker(final String fileName) {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                try(ServerSocket serverSocket = new ServerSocket(25000)){
                System.out.println("listening to port:25000");
                Socket clientSocket = serverSocket.accept();
                System.out.println(clientSocket+" connected.");
                dataInputStream = new DataInputStream(clientSocket.getInputStream());
                dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                int bytes = 0;
                FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                long startTime=System.currentTimeMillis();
                long sizeMax = dataInputStream.readLong();     // read file size
                long size = sizeMax;
                byte[] buffer = new byte[4*1024];
                while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
                    fileOutputStream.write(buffer,0,bytes);
                    size -= bytes;      // read upto file size
                    long currSize= sizeMax-size;
                    updateProgress(currSize,sizeMax);
                    long timePassed=System.currentTimeMillis()-startTime;
                    double speed = (double)currSize/timePassed;
                    long timeRemaining=size/(long)speed;
                    timeRemaining/=1000;
                    updateMessage("Receiving "+String.format("%.2f",(((double)currSize*100)/sizeMax))+"%. Speed: "+String.format("%.2f",speed)+" KB/s.\nTime remaining: "+format(timeRemaining));
                }
                fileOutputStream.close();
                dataInputStream.close();
                dataOutputStream.close();
                clientSocket.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
                return true;
            }
        };
    }
    static String format(long durationSeconds) {
        long durationMillis = TimeUnit.SECONDS.toMillis(durationSeconds);
        // Commons lang:
        return DurationFormatUtils.formatDuration(durationMillis, "HH:mm:ss");
    }
}
