import pandas as pd
import numpy as np
from KalmanFilter import *
from sys import argv

def get_variance(data):
	""" Gets the variance of the circular errors.

	Arguments:

	data - Iterable - Containing dictionaries that include ground truth and circular error information

	Returns:
		The variance of the circular errors.

	"""
	return np.var(data['CE'].tolist())

def KalmanRunner(data, MeasurementVariance , x0 = None, y0 = None, P0 = None):

	# initial position
	if x0 and y0:
		x = np.matrix('{} {} 0. 0.'.format(x0,y0)).T
	else:
		x = np.matrix('0. 0. 0. 0.').T

	# initial uncertainty, np.eye is diag()
	if P0:
		P = np.matrix(np.eye(4))*float(P0)
	else:
		P = np.matrix(np.eye(4))*10

	R = MeasurementVariance * np.matrix(np.eye(2))

	# Parse the values from the pandas dataframe
	observed_x = np.array(data['x'].tolist())
	observed_y = np.array(data['y'].tolist())

	result = []
	for measure in zip(observed_x, observed_y):
		x, P = kalman_xy(x, P, measure, R)
		result.append((x[:2]).tolist())
	kalman_x, kalman_y = zip(*result)

	filtered_x = []
	ce =[]
	for item in kalman_x:
		filtered_x.append(float(str(item).replace('[','').replace(']','')))

	filtered_y = []
	for item in kalman_y:
		filtered_y.append(float(str(item).replace('[','').replace(']','')))

	# Build a dictionary
	d = {
	"observed_x": observed_x,
	"observed_y": observed_y,
	"x": filtered_x,
	"y": filtered_y
	}

	# Put it in a pandas DataFrame
	df = pd.DataFrame(d)
	return df

if __name__ == "__main__":

	positions = pd.io.parsers.read_csv(argv[1])

	if len(argv) < 4:
		print "No variance, initial position or P0 supplied - Will assume the data has circular error information and will calculate measurement variance automatically, assume origin start and a variance of 10"
		KalmanRunner(data = positions, MeasurementVariance = get_variance(positions)).to_csv(argv[2])
	elif len(argv) < 5:
		print "No initial position or P0 supplied - will assume origin start and a variance of 10"
		KalmanRunner(data = positions, MeasurementVariance = float(argv[3])).to_csv(argv[2])
	elif len(argv) > 4:
		KalmanRunner(data = positions, MeasurementVariance = float(argv[3]), x0 = argv[4], y0 = argv[5], P0 = argv[6]).to_csv(argv[2])
