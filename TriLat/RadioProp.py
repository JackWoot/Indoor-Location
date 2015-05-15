"""
Radio Proporgation model
"""
import math

#Define the Bluetooth Low Energy channel frequencies in MHz
Channel_37 = 2402.0
Channel_38 = 2426.0
Channel_39 = 2480.0
Channels = (Channel_37, Channel_38, Channel_39)

#As channel information is not (currently) available, use an average as the transmission frequecy in MHz
T_freq = sum(Channels)/3.0

def circular_error(ground_truth, estimation):

    return math.sqrt(math.pow((ground_truth[0] - estimation[0]),2) + math.pow((ground_truth[1] - estimation[1]),2))

def convert_to_mW(dBm):
    """ Converts the supplied dBm value into milliWatts

    Arguments:
        dBm - float representing the decibels (relative to 1mW)

    Returns:
        A float representing the number of mW the supplied dBm value corresponds to
    """
    return math.pow(10,(dBm/10))


#Define the different radio proporgation models

def free_space(rss, tss):
    """ Method which will calculate the distance based on a free space radio proporgation model.

    Arguments:
        rss - float representing the recieved signal strength in dBm
        tss - float representing the transmitted signal strength in dBm

    Returns:
        A float representing the estimated distance in meters from the transmitter
    """
    if rss > tss:
        return None

    #Set the speed of light in m/s
    c = 2.99792458 * math.pow(10.0,8.0)

    #Convert the dBm values to mW
    rmw = convert_to_mW(rss)
    tmw = convert_to_mW(tss)
    f = T_freq * (10.0 ** 6)
    d = (math.sqrt(tmw-rmw) * c) / (4.0 * math.pi * f)

    return d

def free_space_at_1m(rss, tss):
    """ Method which will calculate the distance based on a free space radio proporgation model assuming a transmitted power level (tss) measured at 1m.

    Arguments:
        rss - float representing the recieved signal strength in dBm
        tss - float representing the transmitted signal strength in dBm at 1m from the

    Returns:
        A float representing the estimated distance in meters from the transmitter or None if the measurment is within 1m.
    """
    #If the recived power is greater that transmitted power then we are inside of 1m
    if rss > tss:
        return None

    #Set the speed of light in m/s
    c = 299792458.0

    #PL = 10.0 * math.log10(convert_to_mW(tss) - convert_to_mW(rss))
    PL = tss - rss
    f = T_freq * (10.0 ** 6)
    const = 20.0 * math.log10(f) - 147.55 #20.0 * math.log10(4.0*math.pi/c)

    #print "### Est Dist ###"
    #print tss
    #print rss
    #print PL
    #print f
    #print const

    #Calculate the distance and add 1m to account for the assumed tss at 1m
    d = math.pow(10.0,((PL - const)/20.0)) + 1.00

    #print d - 1.00

    return d

#Create a dictionary containg all the radio proporgation models
models = dict()
models["FS"] = free_space
models["FS1m"] = free_space_at_1m

def estimate_distance(model, rss, tss):
    """ Converts the supplied recived signal strength into a distance (m) using the
    supplied radio proporgation method.

    Arguments:
        model - String indicating which radio proporgation model to use.
                Avalible models:

                "FS" - Free space proporgation
                "FS1m" - Free space proporgation assuming tss is power at 1m from source

        rss - float representing the recieved signal strength in dBm
        tss - float representing the transmitted signal strength in dBm

    Returns:
        A float representing the estimated distance in meters from the transmitter

    Throws:
        KeyError if the requested model is not valid
    """

    return models[model](rss, tss)
