package info.kgeorgiy.java.advanced.implementor;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.omg.CORBA_2_3.ORB;

import javax.annotation.processing.Completions;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.plugins.bmp.BMPImageWriteParam;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.management.ImmutableDescriptor;
import javax.management.relation.RelationNotFoundException;
import javax.management.remote.rmi.RMIIIOPServerImpl;
import javax.management.remote.rmi.RMIServerImpl;
import javax.naming.ldap.LdapReferralException;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClassImplementorTest extends InterfaceImplementorTest {
    @Test
    public void test07_defaultConstructorClasses() {
        test(false, BMPImageWriteParam.class, RelationNotFoundException.class);
    }

    @Test
    public void test08_noDefaultConstructorClasses() {
        test(false, IIOException.class, ImmutableDescriptor.class, LdapReferralException.class);
    }

    @Test
    public void test09_ambiguousConstructorClasses() {
        test(false, IIOImage.class);
    }

    @Test
    public void test10_utilityClasses() {
        test(true, Completions.class);
    }

    @Test
    public void test11_finalClasses() {
        test(true, Integer.class, String.class);
    }

    @Test
    public void test12_standardNonClasses() {
        test(true, void.class, String[].class, int[].class, String.class, boolean.class);
    }

    @Test
    public void test13_constructorThrows() {
        test(false, FileCacheImageInputStream.class);
    }

    @Test
    public void test14_nonPublicAbstractMethod() {
        test(false, RMIServerImpl.class, RMIIIOPServerImpl.class);
    }

    @Test
    public void test15_inheritedNonPublicAbstractMethod() {
        test(false, ORB.class);
    }
}
