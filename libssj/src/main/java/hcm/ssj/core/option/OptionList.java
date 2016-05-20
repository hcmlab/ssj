package hcm.ssj.core.option;

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
                option.setValue(value);
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
                return option.getValue();
            }
        }
        return null;
    }

    /**
     * @param option Option
     */
    protected final void add(Option option)
    {
        hashSetOptions.add(option);
    }
}
