package potato.cleavefile.controller;

import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Controller
public class CleaveController {

    private Logger log = (Logger) LoggerFactory.getLogger("log");

    @Value("$(file.dir)")
    private String fileDirPathLocation;

    private String inputDir = "input";
    private String outputDir = "output";
    private String processDir = "processing";
    private String archiveDir = "archived";
    private String errorDir = "error";

    @Scheduled(cron = "0 */2 * * * *")
    public void processor() throws IOException {
        for(String fileSourcePathStr : getFiles()) {
            processFile(fileSourcePathStr);
        }
    }

    private void processFile(String fileSourcePathStr) throws IOException {

        Path sourcePath = Paths.get(fileSourcePathStr);
        String filename = sourcePath.getFileName().toString();
        boolean isSuccess = false;

        log.info("File {} cleave initiated :: ", filename );

        Path processingFilePath = moveFile(inputDir, processDir, filename);

        BufferedWriter writer = createBufferedFileWriter(filename);
        try (BufferedReader reader = new BufferedReader(new FileReader(processingFilePath.toFile()))) {
            String readLine = "";
            while ((readLine = reader.readLine()) != null) {
                if (readLine.startsWith("SPLIT;")) {
                    closeWriter(writer);
                    renameOutputFileName(filename, readLine.substring(readLine.lastIndexOf('/') + 1));

                    readLine = reader.readLine();
                    if (readLine != null) {
                        writer = createBufferedFileWriter(filename);
                        writeBufferedLine(writer, readLine);
                    }
                } else {
                    writeBufferedLine(writer, readLine);
                }
            }
            isSuccess = true;
            log.info(" File {} cleaving process is completed successfully", filename);
        } catch (IOException e) {
            moveFile(processDir, errorDir, filename);
            deleteIncompleteOutputFile(filename);
            log.error("Error found in file {} processing", filename, e);
        } finally {
            if (isSuccess) {
                moveFile(processDir, archiveDir, filename);
                log.info("File {} is archived successfully", filename);
            }
        }
    }



    

}
