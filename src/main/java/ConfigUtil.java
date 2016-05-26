import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Created by Nate on 5/23/16.
 */
public class ConfigUtil {
    private static Config conf = null;

    public static Config load(String confFile) {
        if(conf == null) {
            conf = ConfigFactory.load(confFile);
        }
        return conf;
    }

    public static Config getConf() {
        if(conf == null) throw new IllegalStateException("Config not loaded");
        return conf;
    }
}
