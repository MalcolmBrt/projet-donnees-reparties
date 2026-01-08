package services.files;

public interface ServiceFile {
    public byte[] getContent(String fileName) throws Exception;
}