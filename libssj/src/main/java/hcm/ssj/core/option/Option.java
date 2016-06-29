package hcm.ssj.core.option;

/**
 * Standard option for SSJ.<br>
 * Created by Frank Gaibler on 04.03.2016.
 */
public class Option<T>
{
    private final String name;
    private T value;
    private final Class<T> type;
    private final String help;

    /**
     * @param name  String
     * @param value T
     * @param type  Class
     * @param help  String
     */
    public Option(String name, T value, Class<T> type, String help)
    {
        this.name = name;
        this.value = value;
        this.type = type;
        this.help = help;
    }

    /**
     * @return String
     */
    public final String getName()
    {
        return name;
    }

    /**
     * @return T
     */
    public final T getValue()
    {
        return value;
    }

    /**
     * @param value T
     */
    public final void setValue(T value)
    {
        this.value = value;
    }

    /**
     * @return Class
     */
    public final Class<T> getType()
    {
        return type;
    }

    /**
     * @return String
     */
    public final String getHelp()
    {
        return help;
    }
}
