package net.coding.ide.model;

/**
 * Created by tan on 2016/9/21.
 */
public class GitLog {

    private String shortName;

    private String name;

    private String shortMessage;

    private String fullMessage;

    private int commitTime;

    private PersonIdent commiterIdent;

    public static class PersonIdent {
        private String name;

        private String emailAddress;

        public PersonIdent() {

        }

        public PersonIdent(String name, String emailAddress) {
            this.name = name;
            this.emailAddress = emailAddress;
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    public String getFullMessage() {
        return fullMessage;
    }

    public void setFullMessage(String fullMessage) {
        this.fullMessage = fullMessage;
    }

    public int getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(int commitTime) {
        this.commitTime = commitTime;
    }

    public PersonIdent getCommiterIdent() {
        return commiterIdent;
    }

    public void setCommiterIdent(PersonIdent commiterIdent) {
        this.commiterIdent = commiterIdent;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }
}
