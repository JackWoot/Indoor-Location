import numpy as np
import math

def create_A_matrix(points):

    a = list()
    n = len(points)-1
    pn = points[n]

    for i in range(n):
        p = points[i]
        a.append((p[0] - pn[0], p[1] - pn[1]))

    return np.mat(np.vstack((a)))

def create_b_matrix(points, distances):

    b = list()
    n = len(points)-1
    pn = points[n]
    dn = distances[n]

    for i in range(n):
        p = points[i]
        d = distances[i]
        b.append((dn ** 2 - d ** 2 + p[0] ** 2 - pn[0] ** 2 + p[1] ** 2 - pn[1] ** 2))

    return 0.5 * np.mat(np.vstack(b))

def trilaterate(points, distances):
    """ Determines a location based on a list of circle center points and radii (distances from the centrepoint).
    The points and distances lists must be the same length and each distance must correspond to the each center point

    Arguments:
        points - List containing the center points to trilaterate from
        distances - List containing the distances from the unknown point to each of the centre points

    Returns:
        A tuple (x,y) of the estimated crossover point for the trilateration
    """
    if len(points) != len(distances):
        raise RuntimeError("Points and Distance lists must be the same length")

    A = create_A_matrix(points)
    b = create_b_matrix(points, distances)
    P = np.linalg.inv(A.transpose() * A) * A.transpose() * b

    coord = P.ravel().tolist()[0]
    return tuple(coord)

if __name__ == "__main__":

    #### Test with 3 points who intersect at the origin ####

    print "With 3 points - intersect at 0,0"

    P1 = (0.0,1.0)
    P2 = (1.0,0.0)
    P3 = (math.sqrt(2)/2,math.sqrt(2)/2)

    points = (P1,P2,P3)

    d1 = 1.0
    d2 = 1.0
    d3 = 1.0

    distances = (d1,d2,d3)

    points = list(points)
    distances = list(distances)

    print "A:"
    print create_A_matrix(points)
    print "b:"
    print create_b_matrix(points, distances)
    print "P:"
    print trilaterate(points, distances)

    ### Test with extra points ###

    print "With 4 points - intersect at 0,0"

    #Add a 4th point and distance
    points.append((-1.0,-4.0))
    distances.append(math.sqrt(17))

    #Print the components
    print "A:"
    print create_A_matrix(points)
    print "b:"
    print create_b_matrix(points, distances)
    print "P:"
    print trilaterate(points, distances)

    print "With 4 points - intersect at 1,1"

    #Create new points
    points = ((0.0,0.0),(0.0,2.0),(2.0,2.0),(2.0,0.0))
    distances = (math.sqrt(2),math.sqrt(2),math.sqrt(2),math.sqrt(2))

    points = list(points)
    distances = list(distances)

    #Print the components
    print "A:"
    print create_A_matrix(points)
    print "b:"
    print create_b_matrix(points, distances)
    print "P:"
    print trilaterate(points, distances)

    print "With 8 points - intersect at 1,1"

    #Add the 4 new points and distances
    points.append((0.0,1.0))
    points.append((1.0,2.0))
    points.append((2.0,1.0))
    points.append((1.0,0.0))

    for i in range(4):
        distances.append(1.0)

    #Print the components
    print "A:"
    print create_A_matrix(points)
    print "b:"
    print create_b_matrix(points, distances)
    print "P:"
    print trilaterate(points, distances)
