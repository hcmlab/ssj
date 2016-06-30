package hcm.ssj.core.option;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;

/**
 * Derivable option list for SSJ.<br>
 * Created by Frank Gaibler on 04.03.2016.
 */
public abstract class OptionList
{
    protected LinkedHashSet<Option> hashSetOptions = new LinkedHashSet<>();

    /**
     * Used to add all options.
     */
    protected OptionList()
    {
    }

    /**
     * @return Option[]
     */
    public final Option[] getOptions()
    {
        return hashSetOptions.toArray(new Option[hashSetOptions.size()]);
    }

    /**
     * @param name  String
     * @param value Object
     * @return boolean
     */
    public final boolean setOptionValue(String name, Object value)
    {
        for (Option option : hashSetOptions)
        {
            if (option.getName().equals(name))
            {
                option.set(value);
                return true;
            }
        }
        return false;
    }

    /**
     * @param name String
     * @return Object
     */
    public final Object getOptionValue(String name)
    {
        for (Option option : hashSetOptions)
        {
            if (option.getName().equals(name))
            {
                return option.get();
            }
        }
        return null;
    }

    /**
     *
     */
    protected final void addOptions()
    {
        //only add on latest subclass
        if (super.getClass().isAssignableFrom(this.getClass()))
        {
            Field[] fields = this.getClass().getFields();
            for (Field field : fields)
            {
                if (field.getType().isAssignableFrom(Option.class))
                {
                    try
                    {
                        Option option = (Option) field.get(this);
                        //only add instantiated options
                        if (option != null)
                        {
                            add(option);
                        }
                    } catch (IllegalAccessException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @param option Option
     */
    private void add(Option option)
    {
        hashSetOptions.add(option);
    }
}
