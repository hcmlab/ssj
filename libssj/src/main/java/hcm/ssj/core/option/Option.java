package hcm.ssj.core.option;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Util;

/**
 * Standard option for SSJ.<br>
 * Created by Frank Gaibler on 04.03.2016.
 */
public class Option<T>
{
    private final String name;
    private T value;
    private final int size;
    private final Cons.Type type;
    private final String help;

    /**
     * @param name  String
     * @param value T
     * @param type  Cons.Type
     * @param help  String
     */
    public Option(String name, T value, Cons.Type type, String help)
    {
        this(name, value, Util.sizeOf(type), type, help);
    }

    /**
     * @param name  String
     * @param value T
     * @param size  int
     * @param type  Cons.Type
     * @param help  String
     */
    public Option(String name, T value, int size, Cons.Type type, String help)
    {
        this.name = name;
        this.value = value;
        this.size = size;
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
     * @return int
     */
    public final int getSize()
    {
        return size;
    }

    /**
     * @return Cons.Type
     */
    public final Cons.Type getType()
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

//    /**
//     * @param o Object
//     * @return boolean
//     */
//    @Override
//    public final boolean equals(Object o)
//    {
//        return o instanceof Option && ((Option) o).getValue().equals(getValue());
//    }
}
