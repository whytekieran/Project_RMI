package ie.gmit.sw;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.*;
import javax.servlet.http.*;

import ie.gmit.queueModels.QueueFactory;
import ie.gmit.queueModels.QueueType;
import ie.gmit.queueModels.Queueable;
import ie.gmit.queueModels.StringTask;
import ie.gmit.rmiModels.Resultator; 

public class ServiceHandler extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private String remoteHost = null;
	private volatile static long jobNumber = 0;
	
	//Create a queue factory then use it to get a TaskQueue<StringTask>
	//NOTE: changed all queues to static because should only be one and they should be shared..we shall see if it works
	private static QueueFactory qf = QueueFactory.getInstance();
	@SuppressWarnings("unchecked")
	private static Queueable<StringTask> inQueue = qf.getQueue(QueueType.STRING_TASK_QUEUE);
	private static Map<String, Resultator> outQueue = new ConcurrentHashMap<>();
	
	public void init() throws ServletException {
		ServletContext ctx = super.getServletContext();//
		remoteHost = ctx.getInitParameter("RMI_SERVER"); //Reads the value from the <context-param> in web.xml
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		
		//Initialize some request variables with the submitted form info. These are local to this method and thread safe...
		String algorithm = req.getParameter("cmbAlgorithm");
		String str1 = req.getParameter("txtS");
		String str2 = req.getParameter("txtT");
		String taskNumber = req.getParameter("frmTaskNumber");//will be null first time around
		
		out.print("<html><head><title>Distributed Systems Assignment</title>");		
		out.print("</head>");		
		out.print("<body>");
		
		if (taskNumber == null){
			++jobNumber;
			taskNumber = new String("T" + jobNumber);
			inQueue.offer(new StringTask(taskNumber, algorithm, str1, str2));
			outQueue.put(taskNumber, null);
			
			//notes ...also refer to stringRMI project i did myself shows good ideas 
			//need some sort of a client runnable, possibly one for each request here, keeps pulling tasks 
			//from in-queue, then sends data for
			//could have multiple client runnables....or set the thread once then just check if its alive after this
			//comparison to the rmi server, gets resultator object back instantly and adds to the out-queue,
			//keeps asking it the resultator processed. When it is update the out-queue and runnable thread exits.
			//client runnable needs following parameters, string-task, out-queue. This method is thread safe so a new
			//client runnable should be created each time this executes.
			
			
		}else{
			//Check out-queue for finished job
			Resultator r = outQueue.get(taskNumber);
			if(r != null && r.isProcessed()){
				out.print("YOUR RESULT HAS BEEN PROCESSED AND THE RESULT IS: "+r.getResult());
			}
			else{
				out.print("YOUR RESULT IS CURRENTLY BEING PROCESSED");
			}
		}
		
		out.print("<H1>Processing request for Job#: " + taskNumber + "</H1>");
		out.print("<div id=\"r\"></div>");
		
		out.print("<font color=\"#993333\"><b>");
		out.print("RMI Server is located at " + remoteHost);
		out.print("<br>Algorithm: " + algorithm);		
		out.print("<br>String <i>s</i> : " + str1);
		out.print("<br>String <i>t</i> : " + str2);
		out.print("<br>This servlet should only be responsible for handling client request and returning responses. Everything else should be handled by different objects.");
		out.print("Note that any variables declared inside this doGet() method are thread safe. Anything defined at a class level is shared between HTTP requests.");				
		out.print("</b></font>");

		out.print("<P> Next Steps:");	
		out.print("<OL>");
		out.print("<LI>Generate a big random number to use a a job number, or just increment a static long variable declared at a class level, e.g. jobNumber.");	
		out.print("<LI>Create some type of an object from the request variables and jobNumber.");	
		out.print("<LI>Add the message request object to a LinkedList or BlockingQueue (the IN-queue)");			
		out.print("<LI>Return the jobNumber to the client web browser with a wait interval using <meta http-equiv=\"refresh\" content=\"10\">. The content=\"10\" will wait for 10s.");	
		out.print("<LI>Have some process check the LinkedList or BlockingQueue for message requests.");	
		out.print("<LI>Poll a message request from the front of the queue and make an RMI call to the String Comparison Service.");			
		out.print("<LI>Get the <i>Resultator</i> (a stub that is returned IMMEDIATELY by the remote method) and add it to a Map (the OUT-queue) using the jobNumber as the key and the <i>Resultator</i> as a value.");	
		out.print("<LI>Return the result of the string comparison to the client next time a request for the jobNumber is received and the <i>Resultator</i> returns true for the method <i>isComplete().</i>");	
		out.print("</OL>");	
		
		out.print("<form name=\"frmRequestDetails\">");
		out.print("<input name=\"cmbAlgorithm\" type=\"hidden\" value=\"" + algorithm + "\">");
		out.print("<input name=\"txtS\" type=\"hidden\" value=\"" + str1 + "\">");
		out.print("<input name=\"txtT\" type=\"hidden\" value=\"" + str2 + "\">");
		out.print("<input name=\"frmTaskNumber\" type=\"hidden\" value=\"" + taskNumber + "\">");
		out.print("</form>");								
		out.print("</body>");	
		out.print("</html>");	
		
		out.print("<script>");
		out.print("var wait=setTimeout(\"document.frmRequestDetails.submit();\", 10000);");
		System.out.println("SENT");
		out.print("</script>");
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
 	}
}