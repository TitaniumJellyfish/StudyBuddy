
import json, csv, time

def main(): 
    
    fp = open('StudyRooms.csv', 'rU')
    reader = csv.reader(fp)
    nfp = open('FormattedData.csv', 'wb')
    writer =csv.writer(nfp, delimiter=',', quotechar='"', quoting=csv.QUOTE_ALL)
#    updated = time.time()*1000
#    surveys = 1
    header = True
    for row in reader:
        building = row[0]
        room = row[1]
        lat = row[2]
        lng = row[3]
        noise = row[4]
        crowd = row[5]
        rating = row[6]
        capacity = row[7]
        comment = row[8]
        updated = row[9]
        surveys = row[10]
        
        newLine= [building, rating, room +"@" + building, comment, lat, noise, capacity, updated, lng, room, crowd, surveys] 
        writer.writerow(newLine)

main()