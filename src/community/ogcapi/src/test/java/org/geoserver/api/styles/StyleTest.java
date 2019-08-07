/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.util.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class StyleTest extends StylesTestSupport {

    public static final String MY_NEW_STYLE = "myNewStyle";

    @Before
    public void addPondsStyle() throws IOException {
        getTestData().addStyle(SystemTestData.PONDS.getLocalPart(), getCatalog());
        StyleInfo myNewStyle = getCatalog().getStyleByName(MY_NEW_STYLE);
        if (myNewStyle != null) {
            getCatalog().remove(myNewStyle);
        }
    }

    @Test
    public void testGetStyleNative() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("ogc/styles/styles/" + POLYGON_COMMENT);
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        // the native comment got preserved, this is not a re-encoding
        assertThat(response.getContentAsString(), containsString("This is a testable comment"));
    }

    @Test
    public void testGetStyleNativeExplicitFormat() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/styles/styles/"
                                + POLYGON_COMMENT
                                + "?f=application%2Fvnd.ogc.sld%2Bxml");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        // the native comment got preserved, this is not a re-encoding
        assertThat(response.getContentAsString(), containsString("This is a testable comment"));
    }

    @Test
    public void testGetStyleConverted() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/styles/styles/"
                                + POLYGON_COMMENT
                                + "?f=application%2Fvnd.ogc.se%2Bxml");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_11, response.getContentType());
        // the native comment did not get preserved, this is a re-encoding
        assertThat(
                response.getContentAsString(), not(containsString("This is a testable comment")));
        // cannot test further because the SLD 1.1 encoding of styles is not actually available....
    }

    @Test
    public void testPutOnExistingStyle() throws Exception {
        // check initial name (there is none in the initial style)
        StyleInfo ponds = getCatalog().getStyleByName("Ponds");
        assertEquals(null, ponds.getSLD().getStyledLayers()[0].getName());

        String sld = IOUtils.toString(StyleTest.class.getResourceAsStream("simplePoint.sld"));
        MockHttpServletResponse response =
                putAsServletResponse("ogc/styles/styles/Ponds", sld, SLDHandler.MIMETYPE_10);
        assertEquals(204, response.getStatus());

        // check the style got modified
        ponds = getCatalog().getStyleByName("Ponds");
        assertEquals("CookbookSimplePoint", ponds.getSLD().getStyledLayers()[0].getName());
    }

    @Test
    public void testPutOnNewStyle() throws Exception {
        String sld = IOUtils.toString(StyleTest.class.getResourceAsStream("simplePoint.sld"));
        MockHttpServletResponse response =
                putAsServletResponse("ogc/styles/styles/myNewStyle", sld, SLDHandler.MIMETYPE_10);
        assertEquals(204, response.getStatus());

        // check the style got created
        StyleInfo myNewStyle = getCatalog().getStyleByName("myNewStyle");
        assertNotNull(myNewStyle);
        assertEquals("CookbookSimplePoint", myNewStyle.getSLD().getStyledLayers()[0].getName());
    }

    @Test
    public void testValidateOnly() throws Exception {
        // check initial name (there is none in the initial style)
        StyleInfo ponds = getCatalog().getStyleByName("Ponds");
        assertEquals(null, ponds.getSLD().getStyledLayers()[0].getName());

        String sld = IOUtils.toString(StyleTest.class.getResourceAsStream("simplePoint.sld"));
        MockHttpServletResponse response =
                putAsServletResponse(
                        "ogc/styles/styles/Ponds?validate=only", sld, SLDHandler.MIMETYPE_10);
        assertEquals(204, response.getStatus());

        // check the style did not get modified
        ponds = getCatalog().getStyleByName("Ponds");
        assertEquals(null, ponds.getSLD().getStyledLayers()[0].getName());
    }

    @Test
    public void testPutValidateInvalid() throws Exception {
        String sld =
                IOUtils.toString(StyleTest.class.getResourceAsStream("simplePointInvalid.sld"));
        MockHttpServletResponse response =
                putAsServletResponse(
                        "ogc/styles/styles/Ponds?validate=only", sld, SLDHandler.MIMETYPE_10);
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), CoreMatchers.containsString("Mark"));
    }
}
