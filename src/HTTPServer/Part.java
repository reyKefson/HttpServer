package HTTPServer;

public class Part {
    private byte[] content;
    private String fileName;


    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getContent() {
        return this.content;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PartX{");
        sb.append(", content='").append(content).append('\'');
        sb.append(", fileName='").append(fileName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
