package cloud.benchflow.driversmaker.resources;

import com.google.common.io.ByteStreams;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import cloud.benchflow.config.converter.BenchFlowConfigConverter;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
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
                          FormDataContentDisposition benchflowConfigDetail) throws IOException {
        //returns benchflow config converted to faban xml
        byte[] data = ByteStreams.toByteArray(benchflowConfig);
        String yamlConfig = new String(data);
        return bfc.convertAndStringify(yamlConfig);
//        return bfc.from(benchflowConfig);
    }

}
