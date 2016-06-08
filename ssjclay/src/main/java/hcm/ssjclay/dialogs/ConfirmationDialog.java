package hcm.ssjclay.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import hcm.ssj.core.Cons;
import hcm.ssj.core.option.Option;
import hcm.ssjclay.R;

/**
 * A Dialog to confirm actions.<br>
 * Created by Frank Gaibler on 16.09.2015.
 */
public class ConfirmationDialog extends DialogFragment
{
    /**
     * The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     */
    public interface IConfirmationDialog
    {
        /**
         * @param dialog DialogFragment
         */
        void onDialogPositiveClick(DialogFragment dialog);

        /**
         * @param dialog DialogFragment
         */
        void onDialogNegativeClick(DialogFragment dialog);
    }

    private int okMessage = R.string.str_ok;
    private int cancelMessage = R.string.str_cancel;
    private IConfirmationDialog mListener;
    private Option option = null;
    private View inputView = null;
    private Object value = null;

    /**
     * @param activity Activity
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try
        {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (IConfirmationDialog) activity;
        } catch (ClassCastException e)
        {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement " + IConfirmationDialog.class.getSimpleName());
        }
    }

    /**
     * @param savedInstanceState Bundle
     * @return Dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        if (option == null)
        {
            throw new RuntimeException();
        }
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(option != null ? option.getName() : "option");
        builder.setPositiveButton(okMessage, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        //set value
                        switch (option.getType())
                        {
                            case UNDEF:
                                throw new RuntimeException();
                            case BYTE:
                                option.setValue(Byte.valueOf(((TextView) inputView).getText().toString()));
                                break;
                            case SHORT:
                                option.setValue(Short.valueOf(((TextView) inputView).getText().toString()));
                                break;
                            case INT:
                                option.setValue(Integer.valueOf(((TextView) inputView).getText().toString()));
                                break;
                            case LONG:
                                option.setValue(Long.valueOf(((TextView) inputView).getText().toString()));
                                break;
                            case FLOAT:
                                option.setValue(Float.valueOf(((TextView) inputView).getText().toString()));
                                break;
                            case DOUBLE:
                                option.setValue(Double.valueOf(((TextView) inputView).getText().toString()));
                                break;
                            case BOOL:
                                option.setValue(((CheckBox) inputView).isChecked());
                                break;
                            case CHAR:
                                option.setValue(((TextView) inputView).getText().toString().toCharArray()[0]);
                                break;
                            case CUSTOM:
                                Object obj = option.getValue();
                                if (obj != null && obj.getClass().isEnum())
                                {
                                    if (value != null)
                                    {
                                        option.setValue(value);
                                    }
                                } else
                                {
                                    option.setValue(((TextView) inputView).getText().toString());
                                }
                                break;
                            case STRING:
                                option.setValue(((TextView) inputView).getText().toString());
                                break;
                            default:
                                option.setValue(((TextView) inputView).getText().toString());
                                break;
                        }
                        // Send the positive button event back to the host activity
                        mListener.onDialogPositiveClick(ConfirmationDialog.this);
                    }
                }
        );
        builder.setNegativeButton(cancelMessage, new DialogInterface.OnClickListener()

                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // Send the negative button event back to the host activity
                        mListener.onDialogNegativeClick(ConfirmationDialog.this);
                    }
                }

        );
        value = option.getValue();
        // Set up the view
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if (option.getType() == Cons.Type.BOOL)
        {
            //checkbox for boolean values
            inputView = new CheckBox(getActivity());
        } else if (value != null && value.getClass().isEnum())
        {
            //create spinner selection for enums which are not null
            inputView = new Spinner(getActivity());
            Object[] enums = value.getClass().getEnumConstants();
            ((Spinner) inputView).setAdapter(new ArrayAdapter<Object>(
                    getActivity(), android.R.layout.simple_spinner_item, enums));
            //preselect item
            for (int i = 0; i < enums.length; i++)
            {
                if (enums[i].equals(value))
                {
                    ((Spinner) inputView).setSelection(i);
                    break;
                }
            }
            ((Spinner) inputView).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    value = position >= 0 ? parent.getItemAtPosition(position) : null;
                }

                @Override
                public void onNothingSelected(AdapterView parent)
                {
                    value = null;
                }
            });
        } else
        {
            //normal text view for everything else
            inputView = new EditText(getActivity());
        }
        linearLayout.addView(inputView);
        final String helpText = option.getHelp();
        if (!helpText.isEmpty())
        {
            TextView textViewHelp = new TextView(getActivity());
            textViewHelp.setText(helpText);
            linearLayout.addView(textViewHelp);
        }
        // Specify the expected input type
        switch (option.getType())
        {
            case UNDEF:
                throw new RuntimeException();
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
                ((TextView) inputView).setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                ((TextView) inputView).setText(value != null ? value.toString() : "null", TextView.BufferType.NORMAL);
                break;
            case FLOAT:
            case DOUBLE:
                ((TextView) inputView).setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                ((TextView) inputView).setText(value != null ? value.toString() : "null", TextView.BufferType.NORMAL);
                break;
            case BOOL:
                ((CheckBox) inputView).setChecked((Boolean) value);
                break;
            case CHAR:
            case CUSTOM:
                //ignore enums
                if (inputView instanceof Spinner)
                {
                    break;
                }
            case STRING:
            default:
                ((TextView) inputView).setInputType(InputType.TYPE_CLASS_TEXT);
                ((TextView) inputView).setText(value != null ? value.toString() : "null", TextView.BufferType.NORMAL);
                break;
        }
        builder.setView(linearLayout);
        // Create the AlertDialog object and return it
        return builder.create();
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
     * @param option Option
     */
    public void setOption(Option option)
    {
        this.option = option;
    }
}
