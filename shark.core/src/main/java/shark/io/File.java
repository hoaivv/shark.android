package shark.io;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Path;
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

    private File(java.io.File file) {
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
     * Reads json string from file then converts it to an object of a specified type
     * @param type class of object to be extracted from file
     * @param <T> type of object to be extracted from file
     * @return object extracted from file
     * @throws IOException throws if I/O error occurred during reading operation
     */
    public <T> T readObject(Class<T> type) throws IOException {

        Reader reader = null;

        try {

            reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(this)));
            return new Gson().fromJson(reader, type);
        }
        finally {
            if (reader != null) reader.close();
        }
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
     * Writes a string to the file. If the file exists it will be overwritten. If the file does not
     * exist, it will be created.
     * @param text string to be written to the file
     * @throws IOException throws if I/O error occurred during writing operation
     */
    public void writeAllText(String text) throws IOException {
        writeAllBytes(text.getBytes());
    }

    /**
     * Serializes an object as a json string then writes that string to file. If the file exists it
     * will be overwritten. If the file does not exist, it will be created.
     * @param obj object to be serialized as a json string
     * @throws IOException throws if I/O error occurred during writing operation
     */
    public void writeObject(Object obj) throws IOException {
        writeAllBytes(new Gson().toJson(obj).getBytes());
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
     * Deletes the file/directory
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

    /**
     * Returns an array of abstract pathnames denoting the files in the
     * directory denoted by this abstract pathname.
     *
     * <p> If this abstract pathname does not denote a directory, then this
     * method returns {@code null}.  Otherwise an array of {@code File} objects
     * is returned, one for each file or directory in the directory.  Pathnames
     * denoting the directory itself and the directory's parent directory are
     * not included in the result.  Each resulting abstract pathname is
     * constructed from this abstract pathname using the {@link #File(java.io.File,
     * String) File(File,&nbsp;String)} constructor.  Therefore if this
     * pathname is absolute then each resulting pathname is absolute; if this
     * pathname is relative then each resulting pathname will be relative to
     * the same directory.
     *
     * <p> There is no guarantee that the name strings in the resulting array
     * will appear in any specific order; they are not, in particular,
     * guaranteed to appear in alphabetical order.
     *
     * <p> Note that the {@link java.nio.file.Files} class defines the {@link
     * java.nio.file.Files#newDirectoryStream(Path) newDirectoryStream} method
     * to open a directory and iterate over the names of the files in the
     * directory. This may use less resources when working with very large
     * directories.
     *
     * @return  An array of abstract pathnames denoting the files and
     *          directories in the directory denoted by this abstract pathname.
     *          The array will be empty if the directory is empty.  Returns
     *          {@code null} if this abstract pathname does not denote a
     *          directory, or if an I/O error occurs.
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          SecurityManager#checkRead(String)} method denies read access to
     *          the directory
     *
     * @since  1.2
     */
    public File[] listFiles() {
        return convert(super.listFiles());
    }

    /**
     * Returns an array of abstract pathnames denoting the files and
     * directories in the directory denoted by this abstract pathname that
     * satisfy the specified filter.  The behavior of this method is the same
     * as that of the {@link #listFiles()} method, except that the pathnames in
     * the returned array must satisfy the filter.  If the given {@code filter}
     * is {@code null} then all pathnames are accepted.  Otherwise, a pathname
     * satisfies the filter if and only if the value {@code true} results when
     * the {@link FileFilter#accept FileFilter.accept(File)} method of the
     * filter is invoked on the pathname.
     *
     * @param  filter
     *         A file filter
     *
     * @return  An array of abstract pathnames denoting the files and
     *          directories in the directory denoted by this abstract pathname.
     *          The array will be empty if the directory is empty.  Returns
     *          {@code null} if this abstract pathname does not denote a
     *          directory, or if an I/O error occurs.
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          SecurityManager#checkRead(String)} method denies read access to
     *          the directory
     *
     * @since  1.2
     * @see java.nio.file.Files#newDirectoryStream(Path,java.nio.file.DirectoryStream.Filter)
     */
    public File[] listFiles(FileFilter filter) {
        return convert(super.listFiles(filter));
    }

    /**
     * Returns an array of abstract pathnames denoting the files and
     * directories in the directory denoted by this abstract pathname that
     * satisfy the specified filter.  The behavior of this method is the same
     * as that of the {@link #listFiles()} method, except that the pathnames in
     * the returned array must satisfy the filter.  If the given {@code filter}
     * is {@code null} then all pathnames are accepted.  Otherwise, a pathname
     * satisfies the filter if and only if the value {@code true} results when
     * the {@link FilenameFilter#accept
     * FilenameFilter.accept(File,&nbsp;String)} method of the filter is
     * invoked on this abstract pathname and the name of a file or directory in
     * the directory that it denotes.
     *
     * @param  filter
     *         A filename filter
     *
     * @return  An array of abstract pathnames denoting the files and
     *          directories in the directory denoted by this abstract pathname.
     *          The array will be empty if the directory is empty.  Returns
     *          {@code null} if this abstract pathname does not denote a
     *          directory, or if an I/O error occurs.
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          SecurityManager#checkRead(String)} method denies read access to
     *          the directory
     *
     * @since  1.2
     * @see java.nio.file.Files#newDirectoryStream(Path,String)
     */
    public File[] listFiles(FilenameFilter filter) {
        return convert(super.listFiles(filter));
    }

    /**
     * Returns the absolute form of this abstract pathname.  Equivalent to
     * <code>new&nbsp;File(this.{@link #getAbsolutePath})</code>.
     *
     * @return  The absolute abstract pathname denoting the same file or
     *          directory as this abstract pathname
     *
     * @throws  SecurityException
     *          If a required system property value cannot be accessed.
     *
     * @since 1.2
     */
    public File getAbsoluteFile() {
        return new File(super.getAbsoluteFile());
    }

    /**
     * Returns the canonical form of this abstract pathname.  Equivalent to
     * <code>new&nbsp;File(this.{@link #getCanonicalPath})</code>.
     *
     * @return  The canonical pathname string denoting the same file or
     *          directory as this abstract pathname
     *
     * @throws  IOException
     *          If an I/O error occurs, which is possible because the
     *          construction of the canonical pathname may require
     *          filesystem queries
     *
     * @throws  SecurityException
     *          If a required system property value cannot be accessed, or
     *          if a security manager exists and its <code>{@link
     *          java.lang.SecurityManager#checkRead}</code> method denies
     *          read access to the file
     *
     * @since 1.2
     * @see     Path#toRealPath
     */
    public File getCanonicalFile() throws IOException {
        return new File(super.getCanonicalFile());
    }

    /**
     * Returns the abstract pathname of this abstract pathname's parent,
     * or <code>null</code> if this pathname does not name a parent
     * directory.
     *
     * <p> The <em>parent</em> of an abstract pathname consists of the
     * pathname's prefix, if any, and each name in the pathname's name
     * sequence except for the last.  If the name sequence is empty then
     * the pathname does not name a parent directory.
     *
     * @return  The abstract pathname of the parent directory named by this
     *          abstract pathname, or <code>null</code> if this pathname
     *          does not name a parent
     *
     * @since 1.2
     */
    public File getParentFile() {
        return new File(super.getParentFile());
    }


    /**
     * Creates an empty file in the default temporary-file directory, using
     * the given prefix and suffix to generate its name. Invoking this method
     * is equivalent to invoking <code>{@link #createTempFile(java.lang.String,
     * java.lang.String, java.io.File)
     * createTempFile(prefix,&nbsp;suffix,&nbsp;null)}</code>.
     *
     * <p> The {@link
     * java.nio.file.Files#createTempFile(String,String,java.nio.file.attribute.FileAttribute[])
     * Files.createTempFile} method provides an alternative method to create an
     * empty file in the temporary-file directory. Files created by that method
     * may have more restrictive access permissions to files created by this
     * method and so may be more suited to security-sensitive applications.
     *
     * @param  prefix     The prefix string to be used in generating the file's
     *                    name; must be at least three characters long
     *
     * @param  suffix     The suffix string to be used in generating the file's
     *                    name; may be <code>null</code>, in which case the
     *                    suffix <code>".tmp"</code> will be used
     *
     * @return  An abstract pathname denoting a newly-created empty file
     *
     * @throws  IllegalArgumentException
     *          If the <code>prefix</code> argument contains fewer than three
     *          characters
     *
     * @throws  IOException  If a file could not be created
     *
     * @throws  SecurityException
     *          If a security manager exists and its <code>{@link
     *          java.lang.SecurityManager#checkWrite(java.lang.String)}</code>
     *          method does not allow a file to be created
     *
     * @since 1.2
     */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        return new File(java.io.File.createTempFile(prefix, suffix));
    }

    /**
     * <p> Creates a new empty file in the specified directory, using the
     * given prefix and suffix strings to generate its name.  If this method
     * returns successfully then it is guaranteed that:
     *
     * <ol>
     * <li> The file denoted by the returned abstract pathname did not exist
     *      before this method was invoked, and
     * <li> Neither this method nor any of its variants will return the same
     *      abstract pathname again in the current invocation of the virtual
     *      machine.
     * </ol>
     *
     * This method provides only part of a temporary-file facility.  To arrange
     * for a file created by this method to be deleted automatically, use the
     * <code>{@link #deleteOnExit}</code> method.
     *
     * <p> The <code>prefix</code> argument must be at least three characters
     * long.  It is recommended that the prefix be a short, meaningful string
     * such as <code>"hjb"</code> or <code>"mail"</code>.  The
     * <code>suffix</code> argument may be <code>null</code>, in which case the
     * suffix <code>".tmp"</code> will be used.
     *
     * <p> To create the new file, the prefix and the suffix may first be
     * adjusted to fit the limitations of the underlying platform.  If the
     * prefix is too long then it will be truncated, but its first three
     * characters will always be preserved.  If the suffix is too long then it
     * too will be truncated, but if it begins with a period character
     * (<code>'.'</code>) then the period and the first three characters
     * following it will always be preserved.  Once these adjustments have been
     * made the name of the new file will be generated by concatenating the
     * prefix, five or more internally-generated characters, and the suffix.
     *
     * <p> If the <code>directory</code> argument is <code>null</code> then the
     * system-dependent default temporary-file directory will be used.  The
     * default temporary-file directory is specified by the system property
     * <code>java.io.tmpdir</code>.  On UNIX systems the default value of this
     * property is typically <code>"/tmp"</code> or <code>"/var/tmp"</code>; on
     * Microsoft Windows systems it is typically <code>"C:\\WINNT\\TEMP"</code>.  A different
     * value may be given to this system property when the Java virtual machine
     * is invoked, but programmatic changes to this property are not guaranteed
     * to have any effect upon the temporary directory used by this method.
     *
     * @param  prefix     The prefix string to be used in generating the file's
     *                    name; must be at least three characters long
     *
     * @param  suffix     The suffix string to be used in generating the file's
     *                    name; may be <code>null</code>, in which case the
     *                    suffix <code>".tmp"</code> will be used
     *
     * @param  directory  The directory in which the file is to be created, or
     *                    <code>null</code> if the default temporary-file
     *                    directory is to be used
     *
     * @return  An abstract pathname denoting a newly-created empty file
     *
     * @throws  IllegalArgumentException
     *          If the <code>prefix</code> argument contains fewer than three
     *          characters
     *
     * @throws  IOException  If a file could not be created
     *
     * @throws  SecurityException
     *          If a security manager exists and its <code>{@link
     *          java.lang.SecurityManager#checkWrite(java.lang.String)}</code>
     *          method does not allow a file to be created
     *
     * @since 1.2
     */
    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        return new File(java.io.File.createTempFile(prefix, suffix, directory));
    }

    /**
     * Returns the file system roots. On Android and other Unix systems, there is
     * a single root, {@code /}.
     */
    public static File[] listRoots() {
        return convert(java.io.File.listRoots());
    }

    /**
     * Converts instances of {@link java.io.File} to {@link File}
     * @param files instances of {@link java.io.File} to be converted
     * @return instances of converted {@link File }
     */
    public static File[] convert(java.io.File[] files) {

        File[] results = new File[files.length];

        for (int i = 0; i < files.length; i++) results[i] = new File(files[i]);

        return results;
    }

    /**
     * Converts an instance of {@link java.io.File} to {@link File}
     * @param file instance of {@link java.io.File} to be converted
     * @return instance of converted {@link File}
     */
    public static File convert(java.io.File file) {
        return new File(file);
    }
}
