# About
An implementation of the Go-Back-N protocol using Java. The protocol is used to transfer a  
text file from one host to another across an unreliable network. the nEmulator is used to emulate   
network errors, such as packet loss and duplicate packets.   
# How to Run:
1. SSH into school servers using three different terminal windows to obtain three different hosts.
        Example:  
        Terminal Window 1: ubuntu1604-006.student.cs.uwaterloo.ca -> host1  
        Terminal Window 2: ubuntu1604-004.student.cs.uwaterloo.ca -> host2  
        Terminal Window 3: ubuntu1604-008.student.cs.uwaterloo.ca -> host3  

2. Run:  
   > make clean  
3. Run:  
   >make   
4. Obtain 4 different port numbers using the following terminal command 4 times:
    > python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'  
    let  
        9991 -> first port number  
        9992 -> second port number  
        9993 -> third port number  
        9994 -> fourth port number  

5. For the following commands, replace hosts and port numbers with their corresponding values.
6. On host1 (terminal window 1), Run:
    > ./nEmulator-linux386 9991 host2 9994 9993 host3 9992 1 0.2 0  
    Refer to the assignment pdf to change how the behavior of nEmulator-linux386  

7. On host2 (terminal window 2), Run the following with the specified output file name  
    > java Receiver host1 9993 9994 <output File>  

8. On host3 (terminal window 3), Run the following with the specified input file name that you would like to send.  
    > java Sender host1 9991 9992 <input file>  
    -test.txt is a sample file (~47 500byte packets)  
9. Wait for host2 and host3 to finish transferring.  


## Tested Using School CS Servers.  

Example:  
./nEmulator-linux386 60615 ubuntu1604-004.student.cs.uwaterloo.ca 52477 37587     ubuntu1604-008.student.cs.uwaterloo.ca 59555 1 0.2 0  

//receiver  
java Receiver ubuntu1604-006.student.cs.uwaterloo.ca 37587 52477 output.txt  

//Sender  
java Sender ubuntu1604-006.student.cs.uwaterloo.ca 60615 59555 test.txt  

host1-> ubuntu1604-006.student.cs.uwaterloo.ca  
host2-> ubuntu1604-004.student.cs.uwaterloo.ca  
host3-> ubuntu1604-008.student.cs.uwaterloo.ca  
9991 -> 60615  
9992 -> 59555   
9993 -> 37587  
9994 -> 52477  
