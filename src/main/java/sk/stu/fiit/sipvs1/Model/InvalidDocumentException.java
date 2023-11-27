package sk.stu.fiit.sipvs1.Model;

public class InvalidDocumentException extends Exception{

    Exception cause;

    public InvalidDocumentException(String s) {
        super(s);
    }

    public InvalidDocumentException(String s, Exception e) {
        super(s);
        cause = e;
    }
}
