package hcm.ssj.feedback.classes;

import hcm.ssj.feedback.actions.Action;

/**
 * Created by Johnny on 03.06.2016.
 */
public interface FeedbackListener
{
    void onPostFeedback(hcm.ssj.core.event.Event ssjEvent, Action action, float value);
}
