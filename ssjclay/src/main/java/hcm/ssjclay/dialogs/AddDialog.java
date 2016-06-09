package hcm.ssjclay.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import hcm.ssjclay.R;
import hcm.ssjclay.creator.Builder;
import hcm.ssjclay.creator.Linker;

/**
 * A Dialog to confirm actions.<br>
 * Created by Frank Gaibler on 16.09.2015.
 */
public class AddDialog extends DialogFragment
{
    private int titleMessage = R.string.app_name;
    private int okMessage = R.string.str_ok;
    private int cancelMessage = R.string.str_cancel;
    private ArrayList<Class> clazzes = null;
    private ArrayList<Listener> alListeners = new ArrayList<>();
    private ListView listView;

    /**
     * @param savedInstanceState Bundle
     * @return Dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        if (clazzes == null)
        {
            throw new RuntimeException();
        }
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleMessage);
        builder.setPositiveButton(okMessage, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        SparseBooleanArray checked = listView.getCheckedItemPositions();
                        int selected = 0;
                        for (int i = 0; i < listView.getAdapter().getCount(); i++)
                        {
                            if (checked.get(i))
                            {
                                selected++;
                            }
                        }
                        if (clazzes != null && selected > 0)
                        {
                            for (int i = 0; i < listView.getAdapter().getCount(); i++)
                            {
                                if (checked.get(i))
                                {
                                    Linker.getInstance().add(Builder.instantiate(clazzes.get(i)));
                                }
                            }
                            for (Listener listener : alListeners)
                            {
                                listener.onPositiveEvent(null);
                            }
                        } else
                        {
                            for (Listener listener : alListeners)
                            {
                                listener.onNegativeEvent(null);
                            }
                        }
                    }
                }
        );
        builder.setNegativeButton(cancelMessage, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        for (Listener listener : alListeners)
                        {
                            listener.onNegativeEvent(null);
                        }
                    }
                }
        );
        // Set up the input
        listView = new ListView(getContext());
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        if (clazzes != null && clazzes.size() > 0)
        {
            String[] ids = new String[clazzes.size()];
            for (int i = 0; i < ids.length; i++)
            {
                ids[i] = clazzes.get(i).getSimpleName();
            }
            listView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_multiple_choice, ids));
        } else
        {
            listView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_multiple_choice));
        }
        builder.setView(listView);
        // Create the AlertDialog object and return it
        return builder.create();
    }

    /**
     * @return int
     */
    public int getTitleMessage()
    {
        return titleMessage;
    }

    /**
     * @param title int
     */
    public void setTitleMessage(int title)
    {
        this.titleMessage = title;
    }

    /**
     * @return int
     */
    public int getOkMessage()
    {
        return okMessage;
    }

    /**
     * @param okMessage int
     */
    public void setOkMessage(int okMessage)
    {
        this.okMessage = okMessage;
    }

    /**
     * @return int
     */
    public int getCancelMessage()
    {
        return cancelMessage;
    }

    /**
     * @param cancel int
     */
    public void setCancelMessage(int cancel)
    {
        this.cancelMessage = cancel;
    }

    /**
     * @param clazzes Class[]
     */
    public void setOption(ArrayList<Class> clazzes)
    {
        this.clazzes = clazzes;
    }

    /**
     * @param listener Listener
     */
    public void addListener(Listener listener)
    {
        alListeners.add(listener);
    }

    /**
     * @param listener Listener
     */
    public void removeListener(Listener listener)
    {
        alListeners.remove(listener);
    }
}
