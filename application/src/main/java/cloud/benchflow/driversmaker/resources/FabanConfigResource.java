package cloud.benchflow.driversmaker.resources;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import cloud.benchflow.config.converter.BenchFlowConfigConverter;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 06/01/16.
 */
@Path("/convert")
public class FabanConfigResource {

    private final BenchFlowConfigConverter bfc;

    public FabanConfigResource(BenchFlowConfigConverter bfc) { this.bfc = bfc; }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/xml")
    public String convert(@FormDataParam("benchflow-config")
                          InputStream benchflowConfig,
                          @FormDataParam("benchflow-config")
                          FormDataContentDisposition benchflowConfigDetail) {
        //returns benchflow config converted to faban xml
        return bfc.from(benchflowConfig);
    }

}