package com.org.devgenie.controller;

import com.org.devgenie.model.ClassDetails;
import com.org.devgenie.model.PackageDetails;
import com.org.devgenie.parser.JavaCodeParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class DocumentationControllerTest {
    @Mock
    private JavaCodeParser javaCodeParser;
    @InjectMocks
    private DocumentationController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getPackages_addsPackagesAndReturnsIndex() {
        PackageDetails pkg = new PackageDetails("name", "desc");
        when(javaCodeParser.parsePackages()).thenReturn(Collections.singletonList(pkg));
        Model model = new ConcurrentModel();
        String view = controller.getPackages(model);
        assert view.equals("index");
        assert model.getAttribute("packages") != null;
    }

    @Test
    void getPackageDetails_addsClassesAndReturnsPackageDetails() {
        ClassDetails cls = new ClassDetails("name", "desc", List.of(), List.of(), List.of());
        when(javaCodeParser.parseClasses(anyString())).thenReturn(Collections.singletonList(cls));
        Model model = new ConcurrentModel();
        String view = controller.getPackageDetails("pkg", model);
        assert view.equals("package-details");
        assert model.getAttribute("classes") != null;
    }

    @Test
    void getClassDetails_success() throws IOException {
        ClassDetails cls = new ClassDetails("name", "desc", List.of(), List.of(), List.of());
        when(javaCodeParser.parseClassDetails(any(File.class))).thenReturn(cls);
        Model model = new ConcurrentModel();
        String view = controller.getClassDetails("pkg", "Class", model);
        assert view.equals("class-details");
    }

    @Test
    void getClassDetails_failure() throws IOException {
        when(javaCodeParser.parseClassDetails(any(File.class))).thenThrow(new IOException("fail"));
        Model model = new ConcurrentModel();
        String view = controller.getClassDetails("pkg", "Class", model);
        assert view.equals("class-details");
        assert model.getAttribute("message").toString().contains("fail");
    }
}
