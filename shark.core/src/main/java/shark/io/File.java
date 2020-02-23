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

    public File(URI uri) {
        super(uri);
    }

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

    public void writeAllText(String text) throws IOException {
        writeAllBytes(text.getBytes());
    }

    public void writeAllText(String text, String charsetName) throws IOException {
        writeAllBytes(text.getBytes(charsetName));
    }

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

    public void appendAllText(String text) throws IOException {
        appendAllBytes(text.getBytes());
    }

    public void appendAllText(String text, String charsetName) throws IOException {
        appendAllBytes(text.getBytes(charsetName));
    }

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
