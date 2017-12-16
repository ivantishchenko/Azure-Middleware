# ASL 2017

This repository contains the project for the Advanced System Laboratory 2017.

**Legi number:** 17-945-536

**Full name:** Ivan Tishchenko

## Java classes description 

* [RunMW] - Serves as the main entry point for the middle-ware system. Extended boilerplate originally according to the course specification.
* [MiddlewareMain] - Main class of the system. Represents system's architecture and assembles its components.
* [NetThread] - One of the most crucial components of the system's is it's network thread.
* [Worker] - This class represents another important architectural component, namely the Worker Thread.
* [Request] - The class is the representation of incoming requests.
* [Parser] - This class contains helper methods commonly used by other components in the system.
* [CycleCounter] - The class servers as an implementation of the modular counter ( e.g. in case of 6 requests and 3 servers it's distributed to server with IDs as: 0, 1, 2, 0, 1, 2).
* [Statistics] - The object is the structure implementation of the required instrumentation.
* [ShutDownHook] - The code contained in this objected is executed, when user shuts down the middle-ware application.
* [ShutDownHook] - The code contained in this objected is executed, when user shuts down the middle-ware application.
* [Histogram] - The class represents a histogram of request times with the step of 0.1 msec, which is printed on programs shut down.

## Repository structure

**report.pdf** is the the report's pdf is contained in the root .

**build.xml** is the build file for the application. Build the application with **ant jar**

**azure-tmp** directory contains the templates which were used for Azure environment.

**build** directory contains the compiled java classes .class

**dist** contains the final **middleware-tivan.jar** file of out application

**experiments** contains **logfiles** which has all the logfiles for all experiments. contains the journals for experiments and budgeting.

**figuures** contains the xmls of drawings for the report.

**lib** contains jars of dependencies of the project. Namely jars for **Log4J**.

**log** contains the log filels produced after the middleware's run.

**plots** contains the plots used in the report

**resources** contains the XML property file for log4j2

**scripts** contains the scripts for automating the experiments.

**src** directory with source code. The java classes can be found at **src/ch/ethz/asl/main**. The unit tests are located in **src/ch/ethz/asl/test**.

## Logfiles navigation

The logfiles which were used in the report:

#### Section 2, Baseline without middleware.

**Location:** experiments/logfiles/baseline

**Naming convention:** **baseline_X_Y_Z.log**, where **X** is number of firtual clients, **Y** repetition's number, **Z** memtier instance's number

**azure_metrics** - data used for plotting of components utilization

**pings** - pings from the setting

##### Section 2.1, One server

**logfiles_baseline_1_server_GET** - first configuration READ only workload

**logfiles_baseline_1_server_SET** - first configuration WRITE only workload

##### Section 2.2, Two servers

**logfiles_baseline_2_server_GET** - second configuration READ only workload

**logfiles_baseline_2_server_SET** - second configuration WRITE only workload

#### Section 3, Baseline with middleware.

**Location:** experiments/logfiles/baselineMiddleware

**pings** - pings from the setting

##### Section 3.1, One middlware

**logfiles_baselineMW_single_GET_client** - read only, 1 middleware, memtier measurements

**logfiles_baselineMW_single_SET_client** - write only, 1 middleware, memtier measurements

**logfiles_baselineMW_single_GET_MW** - read only, 1 middleware, MW measurements

**logfiles_baselineMW_single_SET_MW** - write only, 1 middleware, MW measurements

##### Section 3.2, Two middlewares

**logfiles_baselineMW_double_GET_client** - read only, 2 middleware, memtier measurements

**logfiles_baselineMW_double_SET_client** - write only, 2 middleware, memtier measurements

**logfiles_baselineMW_double_GET_MW** - read only, 2 middleware, MW measurements

**logfiles_baselineMW_double_SET_MW** - write only, 2 middleware, MW measurements

##### Section 3.2, Two middlewares, additional client machine

**logfiles_baselineMW_double_GET_client_ADD** - Additional client, read only, 2 middleware, memtier measurements

**logfiles_baselineMW_double_SET_client_ADD** - Additional client, write only, 2 middleware, memtier measurements

**logfiles_baselineMW_double_GET_MW_ADD** - Additional client, read only, 2 middleware, MW measurements

**logfiles_baselineMW_double_SET_MW_ADD** - Additional client, write only, 2 middleware, MW measurements

**Naming convention** each directory has **8, 16, 32, 64** subdirectories corresponding to WT. For clienr measurements the convention is **baselineMW_X_Y_Z.log**, **X** is the number of VC, **Y** - repetition's number, **Z** - memtier instance's number. 
For the middleware files the convention is **baselineMW_X_Y_Z.log**, **X** -number of VC, **Y** - middleware instance's number, **Z** - repetition's number.

#### Section 4, Throughput for Writes.

**Location:** experiments/logfiles/throughputWrites

**logfiles_throughputWrites_Clients** - measurements from the client

**logfiles_throughputWrites_MW** - measurements from the MW

**Naming convention** each directory has **8, 16, 32, 64** subdirectories corresponding to WT. For clienr measurements the convention is **throughputWrites_X_Y_Z.log**, **X** is the number of VC, **Y** - repetition's number, **Z** - memtier instance's number. 
For the middleware files the convention is **throughputWrites_X_Y_Z.log**, **X** -number of VC, **Y** - middleware instance's number, **Z** - repetition's number.

#### Section 5, Gets and Multi-gets

**Location:** experiments/logfiles/multiGets

**pings** - pings from the setting

**logfiles_multiGET_nonshard_Client** - nonsharded mode, measurements from memtier
**logfiles_multiGET_nonshard_MW** - nonsharded mode, measurements from Middleware

**logfiles_multiGET_sard_Client** - sharded mode, measurements from memtier
**logfiles_multiGET_shard_MW** - sharded mode, measurements from Middleware

**Naming convention** - all files are named **multi_get_X_Y_Z.log**. **X** the number of multi get keys 1, 3, 6, 9. **Y** - memtier or middleware instance's number. **Z** - repetition's number.

#### Section 6, 2k experiment

**Location:** experiments/logfiles/2k

**pings** - pings from the setting

**logfiles_2k_GET_Client** - read only, client measurements

**logfiles_2k_GET_MW** - read only, MW measurements

**logfiles_2k_SET_Client** - write only, client measurements

**logfiles_2k_SET_MW** - write only, MW measurements

**logfiles_2k_MIX_Client** - write-read, client measurements

**logfiles_2k_MIX_MW** - write-read, MW measurements

**Naming convention** - all files have the format **2k_A_B_C_D_E.log**, where **A** - number of servers, **B** - number of middlewares, C - number of threads, **D** - repetition's number, **C** - memtier or middleware instance's number 