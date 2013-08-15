package de.metalcon.autocompleteServer.Create;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import de.metalcon.autocompleteServer.Helper.ProtocolConstants;

/**
 * 
 * @author Christian Schowalter
 * 
 */
public class ProcessCreateRequest {
	private static final DiskFileItemFactory factory = new DiskFileItemFactory();

	public static ProcessCreateResponse checkRequestParameter(
			HttpServletRequest request, ServletContext context) {
		ProcessCreateResponse response = new ProcessCreateResponse(context);
		System.out.println("HELP");
		CreateRequestContainer suggestTreeCreateRequestContainer = new CreateRequestContainer();

		if (!checkIsMultiPart(request, response)) {
			return response;
		}

		// When Protocol requirements are not met, response is returned to show
		// error message and also inhibit creating corrupt entries.
		final FormItemList items = extractFormItems(request);
		String suggestionString = checkSuggestionString(items, response);
		if (suggestionString == null) {
			suggestTreeCreateRequestContainer = null;
			return response;
		}
		response.addSuggestStringToContainer(suggestionString);
		suggestTreeCreateRequestContainer.setSuggestString(suggestionString);

		String indexName = checkIndexName(items, response);
		// response.addIndexToContainer(indexName);
		suggestTreeCreateRequestContainer.setIndexName(indexName);

		String suggestionKey = checkSuggestionKey(items, response);
		// response.addSuggestionKeyToContainer(suggestionKey);
		suggestTreeCreateRequestContainer.setSuggestString(suggestionKey);

		Integer weight = checkWeight(items, response);
		if (weight == null) {
			suggestTreeCreateRequestContainer = null;
			return response;
		}
		// response.addWeightToContainer(weight);
		suggestTreeCreateRequestContainer.setWeight(weight);

		// Protocol forbids images for suggestions without keys
		// TODO: add this piece of information to nokey-Warning
		if (suggestionKey != null) {
			String image = checkImage(items, response);
			if (image == null) {
				response.addNoImageWarning(CreateStatusCodes.NO_IMAGE);
			}

			suggestTreeCreateRequestContainer.setImageBase64(image);
		}
		response.addContainer(suggestTreeCreateRequestContainer);

		// TODO remove this line, when the queue is ready.
		suggestTreeCreateRequestContainer.run(context);

		return response;
	}

	private static String checkImage(FormItemList items,
			ProcessCreateResponse response) {
		FormFile image = null;
		String imageB64 = null;
		File imageFile = null;
		try {
			// TODO: double check, if it works that way on images
			image = items.getFile(ProtocolConstants.IMAGE);
			imageFile = image.getFile();
		} catch (IllegalArgumentException e) {
			// response.addNoImageWarning(CreateStatusCodes.NO_IMAGE);
			return null;
		}

		BufferedImage bufferedImage = null;
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(imageFile);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return null;
		}
		// maybe there is a better way for verifying image encoding than MIME?
		String mimeType = new MimetypesFileTypeMap().getContentType(imageFile);
		if (!(mimeType.equals("image/JPEG"))) {
			try {
				fileReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		long fileLength = imageFile.length();
		char[] cbuf = null;
		try {
			fileReader.read(cbuf, 0, (int) fileLength);
			fileReader.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		byte[] rawImageData = new byte[(int) fileLength];
		for (int i = 0; i < fileLength; i++) {
			rawImageData[i] = (byte) cbuf[i];
		}
		byte[] base64EncodedImage = Base64.encodeBase64(rawImageData);
		String result = new String(base64EncodedImage);

		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(rawImageData);
			bufferedImage = ImageIO.read(bais);
			// TODO: maybe less strong
			if (!((ProtocolConstants.IMAGE_WIDTH == bufferedImage.getWidth()) && (ProtocolConstants.IMAGE_HEIGHT == bufferedImage
					.getHeight()))) {
				return null;
			}

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return imageB64;
	}

	private static Integer checkWeight(FormItemList items,
			ProcessCreateResponse response) {
		String weight = null;
		try {
			weight = items.getField(ProtocolConstants.SUGGESTION_WEIGHT);
		} catch (IllegalArgumentException e) {
			// TODO: RefactorName
			response.addError(CreateStatusCodes.WEIGHT_NOT_GIVEN + e.toString());
			e.printStackTrace();
			return null;
		}

		try {
			return Integer.parseInt(weight);
		}
		// TODO: verify this is the right exception
		catch (NumberFormatException e) {
			response.addError(CreateStatusCodes.WEIGHT_NOT_A_NUMBER);
			return null;
		}
	}

	private static String checkIndexName(FormItemList items,
			ProcessCreateResponse response) {
		String index = null;
		try {
			index = items.getField(ProtocolConstants.INDEX_PARAMETER);
		} catch (IllegalArgumentException e) {
			response.addDefaultIndexWarning(CreateStatusCodes.INDEXNAME_NOT_GIVEN);
			index = ProtocolConstants.DEFAULT_INDEX_NAME;
		}
		return index;
	}

	private static String checkSuggestionKey(FormItemList items,
			ProcessCreateResponse response) {
		String key = null;
		try {
			key = items.getField(ProtocolConstants.SUGGESTION_KEY);
		}

		// this exception is no reason to abort processing!
		catch (IllegalArgumentException e) {
			response.addSuggestionKeyWarning(CreateStatusCodes.SUGGESTION_KEY_NOT_GIVEN);
			return null;
		}
		//
		if (key.length() > ProtocolConstants.MAX_KEY_LENGTH) {
			response.addError(CreateStatusCodes.KEY_TOO_LONG);
			return null;
		}
		return key;
	}

	private static String checkSuggestionString(FormItemList items,
			ProcessCreateResponse response) {
		String suggestString = null;
		// check is the field exists
		try {
			suggestString = items.getField(ProtocolConstants.SUGGESTION_STRING);
		} catch (IllegalArgumentException e) {
			// TODO: RefactorName
			response.addError(CreateStatusCodes.QUERYNAME_NOT_GIVEN);
			return null;
		}
		// checks if the suggestion String length matches ASTP requirements
		if (suggestString.length() > ProtocolConstants.SUGGESTION_LENGTH) {
			response.addError(CreateStatusCodes.SUGGESTION_STRING_TOO_LONG);
			return null;
		}
		return suggestString;
	}

	// checkContentType(); Accept must be text/x-json.

	/**
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	private static boolean checkIsMultiPart(HttpServletRequest request,
			ProcessCreateResponse response) {
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if (isMultipart) {
			return true;
		}
		response.addError(CreateStatusCodes.REQUEST_MUST_BE_MULTIPART);
		return false;
	}

	private static FormItemList extractFormItems(
			final HttpServletRequest request) {
		final ServletFileUpload upload = new ServletFileUpload(factory);
		final FormItemList formItems = new FormItemList();

		try {
			for (FileItem item : upload.parseRequest(request)) {
				if (item.isFormField()) {
					formItems.addField(item.getFieldName(), item.getString());

					// TODO: remove after debugging!
					System.out.println(item);

				} else {
					formItems.addFile(item.getFieldName(), item);
				}
			}
		} catch (final FileUploadException e) {
			// TODO make status message instead of STrace
			e.printStackTrace();
		} catch (final FormItemDoubleUsageException e) {
			// TODO make status message instead of STrace
			e.printStackTrace();
		}

		return formItems;
	}

}
