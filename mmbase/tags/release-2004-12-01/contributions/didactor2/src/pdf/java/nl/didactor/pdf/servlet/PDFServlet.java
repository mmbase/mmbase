package nl.didactor.pdf.servlet;
import nl.didactor.pdf.PDFConverter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.OutputStream;
import java.net.URL;

public class PDFServlet extends HttpServlet {
    
    public void doGet (HttpServletRequest req, HttpServletResponse resp) 
        throws ServletException, java.io.IOException
    {
        resp.setContentType("application/pdf");      
        String baseUrl = getServletContext().getInitParameter("internalUrl");
        if (baseUrl == null) {
            throw new ServletException("Please set 'internalUrl' in the web.xml!");
        }
        int number = Integer.parseInt(req.getParameter("number"));
        int provider = Integer.parseInt(req.getParameter("provider"));
        URL url = new URL(baseUrl+"/pdf/pdfhtml.jsp?number="+number+"&povider="+provider);
        PDFConverter.pageAsPDF(url, resp.getOutputStream());
   }
}

