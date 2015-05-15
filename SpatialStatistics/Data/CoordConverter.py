import csv

def get_true_xy(trueXYfile):
    #Read in the true xy coordinates
    with open(trueXYFile, 'rb') as fpfile:
        fpreader = csv.DictReader(fpfile)
        fps = dict()
        for row in fpreader:
            fps[int(row["Position"])] = {"x" : float(row["x"]), "y" : float(row["y"])}

    return fps

def transform_coords(rawdata, newfile):
    #Open the raw data file and creat a new data file with each position value replaced with an x y from the fingerprint file
    with open(rawdata, 'rb') as rawfile:
        with open(newfile, 'wb') as newfile:

            #Create the reader for the original file
            rawreader = csv.DictReader(rawfile)

            #Create the writer for the new csv with the x and y coordinates in
            writer = csv.DictWriter(newfile, fieldnames=rawreader.fieldnames)
            writer.writeheader()

            #Cycle through the rows in the old csv and replace the x and y with corrected positions
            for rawrow in rawreader:
                #Replace the x and y in the raw row dictionary with the x and y from the fingerprint dictionary for that position
                rawrow.update(fps[int(rawrow["x"])])
                #Write the new dictionary to the new output file
                writer.writerow(rawrow)
