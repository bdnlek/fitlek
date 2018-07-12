package be.bdnlek.fitlek.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.garmin.fit.ActivityType;

import be.bdnlek.fitlek.FitException;
import be.bdnlek.fitlek.FitRepository;
import be.bdnlek.fitlek.FitService;
import be.bdnlek.fitlek.FitServiceException;
import be.bdnlek.fitlek.IFitRepository;
import be.bdnlek.fitlek.Listener;
import be.bdnlek.fitlek.model.Activity;
import be.bdnlek.fitlek.model.FileList;
import be.bdnlek.fitlek.model.TimeSeries;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api
@Path("/activities")
public class ActivityResource {

	private static Logger LOGGER = Logger.getLogger(ActivityResource.class.getName());
	private IFitRepository repo = new FitRepository("/tmp/fitRepository");

	@ApiOperation(value = "Finds all Activities")
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
	public FileList getActivities() {
		LOGGER.info("-->");
		List<String> fits = repo.getFits();
		LOGGER.info("<--");

		return new FileList(fits);
	}

	@Path("/{id}/{timeSeries}")
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_XML })
	public TimeSeries getTimeSeries(@PathParam("id") Integer id, @PathParam("timeSeries") String timeSeries)
			throws FitServiceException {

		FitService svc = new FitService(repo.getFit(id));
		TimeSeries ts = svc.getActivity(Listener.SUPPORTED_CLASSES).getResults().getTimeSeries(timeSeries);
		if (ts != null) {
			return ts;
		} else {
			throw new NotFoundException();
		}
	}

	@Path("/{id}/metrics")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public String[] getMetrics(@PathParam("id") Integer id) throws FitServiceException {
		FitService svc = new FitService(repo.getFit(id));
		return svc.getActivity(Listener.SUPPORTED_CLASSES).getResults().getTimeSeries();
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public void addActivity(@FormDataParam("file") InputStream is,
			@FormDataParam("file") FormDataContentDisposition fileDisposition) throws FitException {
		File f;
		try {
			String fileName = fileDisposition.getFileName();
			f = File.createTempFile(fileName, "");
			try {
				OutputStream out = null;
				int read = 0;
				byte[] bytes = new byte[1024];

				out = new FileOutputStream(f);
				while ((read = is.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				out.flush();
				out.close();
			} catch (IOException e) {

				e.printStackTrace();
			}
			FitService svc = new FitService(f);
			Activity activity = svc.getActivity();
			if (activity == null
					|| activity.getActivity().equals(ActivityType.getStringFromValue(ActivityType.CYCLING))) {
				throw new FitServiceException("activity is null or not cycling ");
			} else {
				repo.addFitFile(f, fileName);
			}

		} catch (IOException | FitServiceException e) {
			throw new FitException("error at upload", e);
		}

	}

}
