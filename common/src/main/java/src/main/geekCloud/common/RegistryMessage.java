package src.main.geekCloud.common;

public class RegistryMessage extends AbstractMessage{

    private String login;
    private String password;

    public RegistryMessage(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
