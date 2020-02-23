package shark.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;

/**
 * Extends {@link java.io.File} and provides methods to make file read/write operation more
 * convenient
 */
public class File extends java.io.File {

    public File(String pathname) {
        super(pathname);
    }

    public File(String parent, String child) {
        super(parent, child);
    }

    public File(File parent, String child) {
        super(parent, child);
    }

    public File(java.io.File parent, String child) {
        super(parent, child);
    }

    public File(URI uri) {
        super(uri);
    }

    public File(java.io.File file) {
        super(file.toURI());
    }

    /**
     * Read all lines from the file
     * @return a string array, each element of which is a line from the file
     * @throws IOException throws if I/O error occurred during reading operation
     */
    public String[] readAllLines() throws IOException {

        ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = null;

        try {

            reader = new BufferedReader(new FileReader(this));

            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                lines.add(line);
            }
        }
        finally {
            if (reader != null) reader.close();
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Reads all text from the file
     * @return content of the file as a string
     * @throws IOException throws if I/O error occurred during reading operation
     */
    public String readAllText() throws IOException {

        Reader reader = null;
        StringBuilder builder = new StringBuilder();

        try {

            reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(this)), "UTF-8");


            while (true) {

                int character = reader.read();
                if (character < 0) break;

                builder.append((char)character);
            }
        }
        finally {
            if (reader != null) reader.close();
        }

        return builder.toString();
    }

    /**
     * Reads all text from the file
     * @param charsetName name of the charset used to decode file content
     * @return content of the file as a string
     * @throws IOException throws if I/O error occurred during reading operation
     */
    public String readAllText(String charsetName) throws IOException {

        Reader reader = null;
        StringBuilder builder = new StringBuilder();

        try {

            reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(this)), charsetName);


            while (true) {

                int character = reader.read();
                if (character < 0) break;

                builder.append((char)character);
            }
        }
        finally {
            if (reader != null) reader.close();
        }

        return builder.toString();
    }

    /**
     * Reads all bytes from the file
     * @return content of the file as an byte array
     * @throws IOException throws if I/O error occurred during reading operation
     */
    public byte[] readAllBytes() throws IOException {

        FileInputStream reader = null;
        ByteArrayOutputStream stream = null;

        try {

            reader = new FileInputStream(this);
            stream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];

            while (true) {

                int count = reader.read(buffer);
                if (count < 0) break;
                stream.write(buffer,0, count);
            }

            return stream.toByteArray();
        }
        finally {

            if (reader != null) reader.close();
            if (stream != null) stream.close();
        }
    }

    /**
     * Writes an array of bytes to the file. If the file exists it will be overwritten. If the file
     * does not exist, it will be created.
     * @param bytes bytes to be written to the file
     * @throws IOException throws if I/O error occurred during writing operation
     */
    public void writeAllBytes(byte[] bytes) throws IOException {

        OutputStream writer = null;

        try {
            writer = new BufferedOutputStream(new FileOutputStream(this));
            writer.write(bytes);
            writer.flush();
        }
        finally {
            if (writer != null) writer.close();
        }
    }

    /**
     * Write a string to the file. If the file exists it will be overwritten. If the file does not
     * exist, it will be created.
     * @param text string to be written to the file
     * @throws IOException throws if I/O error occurred during writing operation
     */
    public void writeAllText(String text) throws IOException {
        writeAllBytes(text.getBytes());
    }

    /**
     * Write a string to the file. If the file exists it will be overwritten. If the file does not
     * exist, it will be created.
     * @param text string to be written to the file
     * @param charsetName name of the charset to be used to encode the string
     * @throws IOException throws if I/O error occurred during writing operation
     */
    public void writeAllText(String text, String charsetName) throws IOException {
        writeAllBytes(text.getBytes(charsetName));
    }

    /**
     * Appends a byte array to the end of the file. If the file does not exists it will be created.
     * @param bytes an array of bytes to be appended at the end of the file.
     * @throws IOException throws if I/O error occurred during writing operation
     */
    public void appendAllBytes(byte[] bytes) throws IOException {

        OutputStream writer = null;

        try {
            writer = new BufferedOutputStream(new FileOutputStream(this, true));
            writer.write(bytes);
            writer.flush();
        }
        finally {
            if (writer != null) writer.close();
        }
    }

    /**
     * Appends a string to the end of the file. If the file does not exists it will be created.
     * @param text a string to be appended at the end of the file.
     * @throws IOException throws if I/O error occurred during writing operation
     */
    public void appendAllText(String text) throws IOException {
        appendAllBytes(text.getBytes());
    }

    /**
     * Appends a string to the end of the file. If the file does not exists it will be created.
     * @param text a string to be appended at the end of the file.
     * @param charsetName name of the charset to be used to encode the string
     * @throws IOException throws if I/O error occurred during writing operation
     */
    public void appendAllText(String text, String charsetName) throws IOException {
        appendAllBytes(text.getBytes(charsetName));
    }

    /**
     * Deletes a file or directory
     * @param recursive indicates whether the all sub directories and files should be deleted
     *                  or not. This parameter has no effects when deleting a file.
     * @return true if succeed; otherwise false
     */
    public boolean delete(boolean recursive) {

        if (!recursive) return delete();

        for(String name : list()) {

            File file = new File(this + "/" + name);

            if (file.isFile() && !file.delete()) return false;
            if (file.isDirectory() && !file.delete(true)) return false;
        }

        return delete();
    }
}
