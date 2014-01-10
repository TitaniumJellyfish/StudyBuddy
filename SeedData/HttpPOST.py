#populate database from csv file

import urllib, urllib2, csv, json, time
from urllib import urlencode

def main():
    
    values = {}
    keys = ['room_building', 'room_rating', 'comp_name', 'room_comments', 'room_lat', 'room_noise', 
            'room_capacity', 'updated', 'room_lng', 'room_name', 'room_crowd', 'num_surveys']
    file_keys=['Building', 'Rating', 'Room@Building', 'Comment', 'Lat ', 'Noise', 'Capacity', 'Time', 'Lng', 'Room', 'Crowd', 'Surveys']
    url="http://129.170.210.128:8888/upload"
    
    time_keys = ['noisemap_timestamp', 'noisemap_raw', 'noisemap_noise']
    
    #=======Parse the csv file
    fp = open('FormattedData.csv', 'rU')
    updated = int(time.time()*1000)
    raw = True
    
    try:
        reader = csv.DictReader(fp)

        for row in reader:
            #create json for noise
            noise_info = [{time_keys[0]:updated, time_keys[1]:raw, time_keys[2]:int(row['Noise'])}]
            i = 0
            while (i < len(keys)):
                file_key = file_keys[i]
                key = keys[i]
                if (file_key == 'Noise'):
                    values[key] = noise_info
                elif (file_key == 'Building' or file_key =='Room' or file_key == 'Room@Building'):
                    values[key]=row[file_key]
                elif(file_key =='Comment'):
                    values[key]= [row[file_key]]
                elif(file_key == 'Lat ' or file_key =='Lng'):
                    values [key] = float(row[file_key])
                elif (file_key == 'Time'):
                    if (row[file_key] != ""):
                        values[key] = float(row[file_key])
                    else:
                        values[key]=row[file_key]
                else:
                    if (row[file_key] != ""):
                        values[key]=int(row[file_key])
                    else:
                        values[key]=row[file_key]

                i = i+1
#            print values
            data = "["+json.dumps(values, separators=(',',':'))+"]"
            params = [('upload_type', 'ut_room'), ('json_data', data)]

            print data
#            mydata = urlencode(params)
#            req = urllib2.Request(url, mydata);
#            req.add_header("Content-type", "application/x-www-form-urlencoded")
#            urllib2.urlopen(req)

    finally:
        fp.close()

    
main()