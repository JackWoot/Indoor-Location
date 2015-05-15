import numpy as np
import pandas as pd
import rpy2.robjects as robj
import rpy2.robjects.pandas2ri # for dataframe conversion
from rpy2.robjects.packages import importr

def kalman_xy(x, P, measurement, R,
              motion = np.matrix('0. 0. 0. 0.').T,
              Q = np.matrix(np.eye(4))):
    """
    Parameters:
    x: initial state 4-tuple of location and velocity: (x0, x1, x0_dot, x1_dot)
    P: initial uncertainty convariance matrix
    measurement: observed position
    R: measurement noise
    motion: external motion added to state vector x
    Q: motion noise (same shape as P)
    """
    return kalman(x, P, measurement, R, motion, Q,
                  F = np.matrix('''
                      1. 0. 1. 0.;
                      0. 1. 0. 1.;
                      0. 0. 1. 0.;
                      0. 0. 0. 1.
                      '''),
                  H = np.matrix('''
                      1. 0. 0. 0.;
                      0. 1. 0. 0.'''))

def kalman(x, P, measurement, R, motion, Q, F, H):
    '''
    Parameters:
    x: initial state
    P: initial uncertainty convariance matrix
    measurement: observed position (same shape as H*x)
    R: measurement noise
    motion: external motion added to state vector x
    Q: motion noise (same shape as P)
    F: next state function: x_prime = F*x
    H: measurement function: position = H*x

    Return: the updated and predicted new values for (x, P)

    See also http://en.wikipedia.org/wiki/Kalman_filter

    This version of kalman can be applied to many different situations by
    appropriately defining F and H
    '''
    # UPDATE x, P based on measurement m
    # distance between measured and current position-belief
    y = np.matrix(measurement).T - H * x
    S = H * P * H.T + R  # residual convariance
    K = P * H.T * S.I    # Kalman gain
    x = x + K*y
    I = np.matrix(np.eye(F.shape[0])) # identity matrix
    P = (I - K*H)*P

    # PREDICT x, P based on motion
    x = F*x + motion
    P = F*P*F.T + Q

    return x, P

def plot_kalman(data, save = False):
    # Make an robject containing a function that makes the plot.
    # the language in the function is pure R, so it can be anything
    # note that the R environment is blank to start, so ggplot2 has to be
    # loaded
    if (save):
        plotFunc = robj.r("""
         library(ggplot2)

        function(df){
         p <- ggplot(df, aes(x = observed_x, y = observed_y)) +
             geom_point() +
             geom_line(aes(x = kalman_x, y = kalman_y)) +
             theme_bw() +
             ggtitle("Observed vs. Kalman Filtered positions")

         ggsave(kalmanFilter.pdf)
         }
        """)
    else:
        plotFunc = robj.r("""
         library(ggplot2)

        function(df){
         p <- ggplot(df, aes(x = observed_x, y = observed_y)) +
             geom_point() +
             geom_line(aes(x = kalman_x, y = kalman_y)) +
             theme_bw() +
             ggtitle("Observed vs. Kalman Filtered positions")

        print(p)
         }
        """)


    # import graphics devices. This is necessary to shut the graph off
    # otherwise it just hangs and freezes python
    gr = importr('grDevices')

    # convert the testData to an R dataframe
    robj.pandas2ri.activate()
    Data_R = robj.conversion.py2ri(data)

    # run the plot function on the dataframe
    plotFunc(Data_R)

    # ask for input. This requires you to press enter, otherwise the plot
    # window closes immediately
    raw_input()

    # shut down the window using dev_off()
    gr.dev_off()


def demo_kalman_xy():

    x = np.matrix('0. 0. 0. 0.').T 
    P = np.matrix(np.eye(4))*1000 # initial uncertainty, np.eye is identity

    N = 20
    true_x = np.linspace(0.0, 10.0, N) # This is like seq()
    true_y = true_x**2
    observed_x = true_x + 0.05*np.random.random(N)*true_x # random.random generates a U[0,1]
    observed_y = true_y + 0.05*np.random.random(N)*true_y

    result = []
    R = 0.01**2*np.matrix(np.eye(2)) # Measurement variance
    for meas in zip(observed_x, observed_y):
        x, P = kalman_xy(x, P, meas, R)
        result.append((x[:2]).tolist())
    kalman_x, kalman_y = zip(*result)

    filtered_x = []
    for item in kalman_x:
        filtered_x.append(float(str(item).replace('[','').replace(']','')))

    filtered_y = []
    for item in kalman_y:
        filtered_y.append(float(str(item).replace('[','').replace(']','')))

    # First build a dictionary
    d = {
        "observed_x": observed_x,
        "observed_y": observed_y,
        "kalman_x": filtered_x,
        "kalman_y": filtered_y
        }

    # Put it in a pandas DataFrame
    df = pd.DataFrame(d)
    return df
