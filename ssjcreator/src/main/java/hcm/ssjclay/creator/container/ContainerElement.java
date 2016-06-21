package hcm.ssjclay.creator.container;

import java.util.HashMap;

import hcm.ssj.core.Provider;

/**
 * Container element for pipeline builder.<br>
 * Created by Frank Gaibler on 10.03.2016.
 */
public class ContainerElement<T>
{
    private T element;
    private double frameSize = 1;
    private double delta = 0;
    private HashMap<Provider, Boolean> hmProviders = new HashMap<>();

    /**
     * @param element T
     */
    public ContainerElement(T element)
    {
        this.element = element;
    }

    /**
     * @return T
     */
    public T getElement()
    {
        return element;
    }

    /**
     * @return double
     */
    public double getFrameSize()
    {
        return frameSize;
    }

    /**
     * @param frameSize double
     */
    public void setFrameSize(double frameSize)
    {
        this.frameSize = frameSize;
    }

    /**
     * @return double
     */
    public double getDelta()
    {
        return delta;
    }

    /**
     * @param delta double
     */
    public void setDelta(double delta)
    {
        this.delta = delta;
    }

    /**
     * @return HashMap
     */
    public HashMap<Provider, Boolean> getHmProviders()
    {
        return hmProviders;
    }

    /**
     * @param provider Provider
     * @return boolean
     */
    public boolean setAdded(Provider provider)
    {
        return hmProviders.containsKey(provider) && !hmProviders.put(provider, true);
    }

    /**
     * @return boolean
     */
    public boolean allAdded()
    {
        for (boolean value : hmProviders.values())
        {
            if (!value)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @param provider Provider
     * @return boolean
     */
    public boolean addProvider(Provider provider)
    {
        return !hmProviders.containsKey(provider) && hmProviders.put(provider, false) == null;
    }

    /**
     * @param provider Provider
     * @return boolean
     */
    public boolean removeProvider(Provider provider)
    {
        return hmProviders.remove(provider) != null;
    }
}
