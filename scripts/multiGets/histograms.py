import matplotlib.pyplot as plt
import numpy as np
from statistics import stdev

class Histogram:

    KEYS_NUM = 6
    MEMTIER_NUM = 6
    MW_NUMBER = 2
    LOGFILES_PATH = "/home/ivan/asl-fall17-project/experiments/logfiles/multiGets/logfiles_multiGET_nonshard_Client"
    REP_NUMBER = 3
    INSIDE_MW = True

    def __init__(self, path, flag):
        self.LOGFILES_PATH = path
        self.INSIDE_MW = flag

    HISTOGRAM_LEN = 60
    TIME = 60

    def cdfToPdf(self, response_percentage, total_num):
        #print("Repsonse time 0" + str(response_percentage[0]))
        pdf = [0] * len(response_percentage)
        pdf[0] = ((response_percentage[0] * total_num) / 100.0) * self.TIME
        #print("PDF 0 " + str(pdf[0]))

        for i in range(1, len(pdf)):
            pdf[i] = (((response_percentage[i] - response_percentage[i - 1]) * total_num) / 100.0) * self.TIME
            # pdf[i] = (((response_number[i] * total_num) / 100) * self.TIME ) - pdf[i - 1]
            #print("PDF  " + str(i) +  "  "+str(pdf[i]))
        return pdf


    def extractParamsClient(self, logfile):
        #print(logfile)

        response_time_gets = []
        response_number_gets = []

        response_time_sets = []
        response_number_sets = []

        SET_NUM = 0
        GET_NUM = 0

        file = open(logfile, 'r')
        for line in file:
            if line.startswith("Gets"):
                GET_NUM = float(line.split()[1])
                #print(GET_NUM)
            if line.startswith("Sets"):
                SET_NUM = float(line.split()[1])
                #print(SET_NUM)
            if line.startswith("Request Latency Distribution"):
                for line in file:
                    if line.startswith("SET"):
                        response_time = line.split()[1]
                        response_number = line.split()[2]

                        response_time_sets.append(float(response_time))
                        response_number_sets.append(float(response_number))

                    elif line.startswith("GET"):
                        response_time = line.split()[1]
                        response_number = line.split()[2]

                        response_time_gets.append(float(response_time))
                        response_number_gets.append(float(response_number))

        file.close()


        #print(len(response_number_gets))
        pdf_SET = self.cdfToPdf(response_number_sets, SET_NUM)
        pdf_GET = self.cdfToPdf(response_number_gets, GET_NUM)


        out_gets = []
        out_sets = []
        out_hist = []
        step = 0.0


        response_time_list = []
        response_number_list = []
        for i in range(self.HISTOGRAM_LEN):
            out_sets.append((step, np.interp(step, response_time_sets, pdf_SET)))
            out_gets.append((step, np.interp(step, response_time_gets, pdf_GET)))
            out_hist.append((step, (np.interp(step, response_time_gets, pdf_GET) + np.interp(step, response_time_gets, pdf_GET)) / 2  ))


            response_time_list.append(step)
            response_number_list.append((np.interp(step, response_time_sets, pdf_SET) + np.interp(step, response_time_gets, pdf_GET)) / 2)
            step += 0.1



        #print(out_hist)

        # histogram_SET = {}
        # histogram_GET = {}
        #
        # for i in range(len(response_time_gets)):
        #     if pdf_GET[i] != 0:
        #         histogram_GET[response_time_gets[i]] = pdf_GET[i]
        #         print((response_time_gets[i], pdf_GET[i]))
        #
        # print("")
        # for i in range(len(response_time_sets)):
        #     if pdf_SET[i] != 0:
        #         histogram_SET[response_time_sets[i]] = pdf_SET[i]
        #         print((response_time_sets[i], pdf_SET[i]))
        #
        # min_SET = min(histogram_SET, key=histogram_SET.get)
        # min_GET = min(histogram_GET, key=histogram_GET.get)
        #
        # min_glob = min(min_SET, min_GET)
        # OUT = []
        # for i in range(self.HISTOGRAM_LEN):
        #     if min_glob not in histogram_SET and min_glob in histogram_GET:
        #         OUT.append(histogram_GET[min_glob])
        #     elif min_glob not in histogram_GET and min_glob in histogram_SET:
        #         OUT.append(histogram_SET[min_glob])
        #     elif min_glob in histogram_GET and min_glob in histogram_SET:
        #         OUT.append((histogram_SET[min_glob] + histogram_GET[min_glob]) / 2)
        #     min_glob += 0.1
        #
        # print(OUT)
        return response_time_list, response_number_list

    def extractParams(self, logfile):

        response_time_list = []
        response_number_list = []

        file = open(logfile, 'r')
        for line in file:
            if line.startswith("Histogram response times"):
                next(file)
                for line in file:
                    response_time = line.split()[0]
                    response_number = line.split()[3]

                    response_time_list.append(response_time)
                    response_number_list.append(response_number)

        file.close()

        return response_time_list, response_number_list

    def plot_histogram_client(self, filename):
        OUT_TIMES = []
        OUT_NUMBERS = []

        if self.INSIDE_MW == True:
            self.MACHINES_NUMBER = self.MW_NUMBER
        elif self.INSIDE_MW == False:
            self.MACHINES_NUMBER = self.MEMTIER_NUM

        R_STD = []
        for i in range(self.HISTOGRAM_LEN):

            final_response_number = 0
            avg_response_number_vals = []
            for repetition in range(1, self.REP_NUMBER + 1):
                avg_response_number = 0
                for machine in range(1, self.MACHINES_NUMBER + 1):

                    logfile_name = self.LOGFILES_PATH + "/multi_get_{}_{}_{}.log".format(self.KEYS_NUM, repetition, machine)

                    response_time, response_number = self.extractParamsClient(logfile_name)
                    #print("Configuration: " + "keys = {} repetition = {} machine = {}".format(self.KEYS_NUM, repetition, machine))

                    #print(response_time[i])
                    #print(response_number[i])

                    avg_response_number += int(response_number[i])


                avg_response_number /= self.MACHINES_NUMBER
                final_response_number += avg_response_number
                avg_response_number_vals.append(avg_response_number)

            R_STD.append(stdev(avg_response_number_vals))
            final_response_number /= self.REP_NUMBER
            OUT_NUMBERS.append(float(final_response_number))
            OUT_TIMES.append(float(response_time[i]))

        OUT_HISTO = []
        for i in range(self.HISTOGRAM_LEN):
            element = (OUT_TIMES[i], OUT_NUMBERS[i])
            # rint(element)
            OUT_HISTO.append(element)


        # print(OUT_TIMES)
        # print(OUT_NUMBERS)
        # print(len(OUT_NUMBERS))
        # print(len(OUT_TIMES))
        # print(len(R_STD))

        plt.bar(OUT_TIMES, OUT_NUMBERS, 0.1, yerr=R_STD, error_kw=dict(ecolor='red', lw=1, capsize=2, capthick=2))
        plt.savefig(filename)
        plt.gcf().clear()
        print(max(OUT_NUMBERS))

    def plot_histogram_MW(self, filename):
        # Build
        OUT_TIMES = []
        OUT_NUMBERS = []

        if self.INSIDE_MW == True:
            self.MACHINES_NUMBER = self.MW_NUMBER
        elif self.INSIDE_MW == False:
            self.MACHINES_NUMBER = self.MEMTIER_NUM

        R_STD = []
        for i in range(self.HISTOGRAM_LEN):

            final_response_number = 0
            avg_response_number_vals = []
            for repetition in range(1, self.REP_NUMBER + 1):
                avg_response_number = 0
                for machine in range(1, self.MACHINES_NUMBER + 1):

                    logfile_name = self.LOGFILES_PATH + "/multi_get_{}_{}_{}.log".format(self.KEYS_NUM, repetition, machine)

                    response_time, response_number = self.extractParams(logfile_name)
                    #print("Configuration: " + "keys = {} repetition = {} machine = {}".format(self.KEYS_NUM, repetition, machine))

                    #print(response_time[i])
                    #print(response_number[i])

                    avg_response_number += int(response_number[i])


                avg_response_number /= self.MACHINES_NUMBER
                final_response_number += avg_response_number
                avg_response_number_vals.append(avg_response_number)

            R_STD.append(stdev(avg_response_number_vals))
            final_response_number /= self.REP_NUMBER
            OUT_NUMBERS.append(float(final_response_number))
            OUT_TIMES.append(float(response_time[i]))




        OUT_HISTO = []
        for i in range(self.HISTOGRAM_LEN):
            element = (OUT_TIMES[i], OUT_NUMBERS[i])
            # print(element)
            OUT_HISTO.append(element)


        # print(OUT_TIMES)
        # print(OUT_NUMBERS)
        # print(len(OUT_NUMBERS))
        # print(len(OUT_TIMES))
        # print(len(R_STD))

        OUT_NUMBERS = [x / 10 for x in OUT_NUMBERS]
        R_STD = [x / 10 for x in R_STD]

        # fig, ax = plt.subplots()
        # ax.bar(OUT_TIMES, OUT_NUMBERS, 0.1, yerr=R_STD, error_kw=dict(ecolor='red', lw=1, capsize=2, capthick=2))
        # ax.set_yticks(OUT_NUMBERS)
        # plt.savefig(filename)
        plt.bar(OUT_TIMES, OUT_NUMBERS, 0.1, yerr=R_STD, error_kw=dict(ecolor='red', lw=1, capsize=2, capthick=2))
        plt.savefig(filename)
        plt.gcf().clear()
        print(max(OUT_TIMES))


path = "/home/ivan/asl-fall17-project/experiments/logfiles/multiGets/logfiles_multiGET_nonshard_Client"
inside_MW = False
h = Histogram(path, inside_MW)
h.plot_histogram_client("6_keys_nonshard_client.png")

path = "/home/ivan/asl-fall17-project/experiments/logfiles/multiGets/logfiles_multiGET_nonshard_MW"
inside_MW = True
h = Histogram(path, inside_MW)
h.plot_histogram_MW("6_keys_nonshard_MW.png")

