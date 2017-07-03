package net.corda.carpenter

import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.amqp.*
import org.junit.Test
import net.corda.carpenter.test.*
import net.corda.carpenter.MetaCarpenter
import kotlin.test.*

/*******************************************************************************************************/

@CordaSerializable
interface J {
    val j : Int
}

/*******************************************************************************************************/

@CordaSerializable
interface I {
    val i : Int
}

@CordaSerializable
interface II {
    val ii : Int
}

@CordaSerializable
interface III : I {
    val iii : Int
    override val i: Int
}

@CordaSerializable
interface IIII {
    val iiii : Int
    val i    : I
}

/*******************************************************************************************************/

class InheritanceSchemaToClassCarpenterTests : AmqpCarpenterBase() {

    @Test
    fun interfaceParent1() {
        val testJ = 20

        class A(override val j: Int) : J

        val a = A(testJ)

        assertEquals(testJ, a.j)

        val obj = DeserializationInput(factory).deserializeAndReturnEnvelope(serialise(a))

        assert(obj.obj is A)

        val serSchema = obj.envelope.schema

        assertEquals(2, serSchema.types.size)

        val l1 = serSchema.carpenterSchema()

        /* since we're using an envelope generated by seilaising classes defined locally
           it's extremely unlikely we'd need to carpent any classes */
        assertEquals(0, l1.size)

        val curruptSchema = serSchema.curruptName (listOf (classTestName ("A")))

        val l2 = curruptSchema.carpenterSchema()

        assertEquals(1, l2.size)
        val aSchema = l2.carpenterSchemas.find { it.name == curruptName(classTestName("A")) }

        assertNotEquals (null, aSchema)

        assertEquals(curruptName(classTestName("A")), aSchema!!.name)
        assertEquals(1, aSchema.interfaces.size)
        assertEquals(net.corda.carpenter.J::class.java, aSchema.interfaces[0])

        val aBuilder = ClassCarpenter().build(aSchema)

        val objJ = aBuilder.constructors[0].newInstance(testJ)
        val j = objJ as J

        assertEquals(aBuilder.getMethod("getJ").invoke(objJ), testJ)
        assertEquals(a.j, j.j)
    }


    @Test
    fun interfaceParent2() {
        val testJ = 20
        val testJJ = 40

        class A(override val j: Int, val jj: Int) : J

        val a = A(testJ, testJJ)

        assertEquals(testJ, a.j)
        assertEquals(testJJ, a.jj)

        val obj = DeserializationInput(factory).deserializeAndReturnEnvelope(serialise(a))

        assert(obj.obj is A)

        val serSchema = obj.envelope.schema

        assertEquals(2, serSchema.types.size)

        val l1 = serSchema.carpenterSchema()

        /* since we're using an envelope generated by seilaising classes defined locally
           it's extremely unlikely we'd need to carpent any classes */
        assertEquals(0, l1.size)

        val curruptSchema = serSchema.curruptName (listOf (classTestName("A")))
        val aName = curruptName(classTestName("A"))

        val l2 = curruptSchema.carpenterSchema()

        assertEquals(1, l2.size)

        val aSchema = l2.carpenterSchemas.find { it.name == aName }

        assertNotEquals(null, aSchema)

        assertEquals(aName, aSchema!!.name)
        assertEquals(1, aSchema.interfaces.size)
        assertEquals(net.corda.carpenter.J::class.java, aSchema.interfaces[0])

        val aBuilder = ClassCarpenter().build(aSchema)

        val objJ = aBuilder.constructors[0].newInstance(testJ, testJJ)
        val j = objJ as J

        assertEquals(aBuilder.getMethod("getJ").invoke(objJ), testJ)
        assertEquals(aBuilder.getMethod("getJj").invoke(objJ), testJJ)

        assertEquals(a.j, j.j)
    }

    @Test
    fun multipleInterfaces() {
        val testI  = 20
        val testII = 40

        class A(override val i: Int, override val ii: Int) : I, II

        val a  = A(testI, testII)
        val obj = DeserializationInput(factory).deserializeAndReturnEnvelope(serialise(a))

        assert(obj.obj is A)

        val serSchema = obj.envelope.schema

        assertEquals(3, serSchema.types.size)

        val l1 = serSchema.carpenterSchema()

        /* since we're using an envelope generated by seilaising classes defined locally
           it's extremely unlikely we'd need to carpent any classes */
        assertEquals(0, l1.size)

        /* pretend we don't know the class we've been sent, i.e. it's unknown to the class loader, and thus
           needs some carpentry */

        val curruptSchema = serSchema.curruptName (listOf (classTestName ("A")))
        val l2    = curruptSchema.carpenterSchema()
        val aName = curruptName(classTestName("A"))

        assertEquals(1, l2.size)

        val aSchema = l2.carpenterSchemas.find { it.name == aName }

        assertNotEquals(null, aSchema)

        assertEquals(aName, aSchema!!.name)
        assertEquals(2, aSchema.interfaces.size)
        assert (net.corda.carpenter.I::class.java in aSchema.interfaces)
        assert (net.corda.carpenter.II::class.java in aSchema.interfaces)

        val aBuilder = ClassCarpenter().build(aSchema)

        val objA = aBuilder.constructors[0].newInstance(testI, testII)
        val i  = objA as I
        val ii = objA as II

        assertEquals(aBuilder.getMethod("getI").invoke(objA), testI)
        assertEquals(aBuilder.getMethod("getIi").invoke(objA), testII)

        assertEquals(a.i,  i.i)
        assertEquals(a.ii, ii.ii)
    }

    @Test
    fun nestedInterfaces() {
        val testI    = 20
        val testIII  = 60

        class A(override val i: Int, override val iii : Int) : III

        val a  = A(testI, testIII)
        val obj = DeserializationInput(factory).deserializeAndReturnEnvelope(serialise(a))

        assert(obj.obj is A)

        val serSchema = obj.envelope.schema

        assertEquals(3, serSchema.types.size)

        val l1 = serSchema.carpenterSchema()

        /* since we're using an envelope generated by seilaising classes defined locally
           it's extremely unlikely we'd need to carpent any classes */
        assertEquals(0, l1.size)

        val curruptSchema = serSchema.curruptName (listOf (classTestName ("A")))
        val l2 = curruptSchema.carpenterSchema()
        val aName = curruptName(classTestName("A"))

        assertEquals(1, l2.size)

        val aSchema = l2.carpenterSchemas.find { it.name == aName }

        assertNotEquals(null, aSchema)

        assertEquals(aName, aSchema!!.name)
        assertEquals(2, aSchema.interfaces.size)
        assert (net.corda.carpenter.I::class.java in aSchema.interfaces)
        assert (net.corda.carpenter.III::class.java in aSchema.interfaces)

        val aBuilder = ClassCarpenter().build(aSchema)

        val objA = aBuilder.constructors[0].newInstance(testI, testIII)
        val i   = objA as I
        val iii = objA as III

        assertEquals(aBuilder.getMethod("getI").invoke(objA), testI)
        assertEquals(aBuilder.getMethod("getIii").invoke(objA), testIII)

        assertEquals(a.i,   i.i)
        assertEquals(a.i,   iii.i)
        assertEquals(a.iii, iii.iii)
    }

    @Test
    fun memberInterface() {
        val testI     = 25
        val testIIII  = 50

        class A(override val i: Int) : I
        class B(override val i : I, override val iiii : Int) : IIII

        val a = A(testI)
        val b = B(a, testIIII)

        val obj = DeserializationInput(factory).deserializeAndReturnEnvelope(serialise(b))

        assert(obj.obj is B)

        val serSchema = obj.envelope.schema

        /*
         * class A
         * class A's interface (class I)
         * class B
         * class B's interface (class IIII)
         */
        assertEquals(4, serSchema.types.size)

        val curruptSchema = serSchema.curruptName (listOf (classTestName ("A"), classTestName ("B")))
        val cSchema       = curruptSchema.carpenterSchema()
        val aName         = curruptName(classTestName("A"))
        val bName         = curruptName(classTestName("B"))

        assertEquals(2, cSchema.size)

        val aCarpenterSchema = cSchema.carpenterSchemas.find { it.name == aName }
        val bCarpenterSchema = cSchema.carpenterSchemas.find { it.name == bName }

        assertNotEquals (null, aCarpenterSchema)
        assertNotEquals (null, bCarpenterSchema)

        val cc  = ClassCarpenter()
        val cc2 = ClassCarpenter()

        val bBuilder = cc.build(bCarpenterSchema!!)
        bBuilder.constructors[0].newInstance(a, testIIII)

        val aBuilder = cc.build(aCarpenterSchema!!)
        val objA = aBuilder.constructors[0].newInstance(testI)

        /* build a second B this time using our constructed instane of A and not the
           local one we pre defined */
        bBuilder.constructors[0].newInstance(objA, testIIII)

        /* whittle and instantiate a different A with a new class loader */
        val aBuilder2 = cc2.build(aCarpenterSchema)
        val objA2 = aBuilder2.constructors[0].newInstance(testI)

        bBuilder.constructors[0].newInstance(objA2, testIIII)
    }

    /* if we remove the neted interface we should get an error as it's impossible
        to have a concrete class loaded without having access to all of it's elements */
    @Test(expected = UncarpentableException::class)
    fun memberInterface2() {
        val testI     = 25
        val testIIII  = 50

        class A(override val i: Int) : I
        class B(override val i : I, override val iiii : Int) : IIII

        val a = A(testI)
        val b = B(a, testIIII)

        val obj = DeserializationInput(factory).deserializeAndReturnEnvelope(serialise(b))

        assert(obj.obj is B)

        val serSchema = obj.envelope.schema

        /*
         * The classes we're expecting to find:
         *   class A
         *   class A's interface (class I)
         *   class B
         *   class B's interface (class IIII)
         */
        assertEquals(4, serSchema.types.size)

        /* ignore the return as we expect this to throw */
        serSchema.curruptName (listOf (
                classTestName("A"), "${this.javaClass.`package`.name}.I")).carpenterSchema()
    }

    @Test
    fun interfaceAndImplementation() {
        val testI     = 25

        class A(override val i: Int) : I

        val a = A(testI)

        val obj = DeserializationInput(factory).deserializeAndReturnEnvelope(serialise(a))

        assert(obj.obj is A)

        val serSchema = obj.envelope.schema

        /*
         * The classes we're expecting to find:
         *   class A
         *   class A's interface (class I)
         */
        assertEquals(2, serSchema.types.size)

        val amqpSchema = serSchema.curruptName (listOf (classTestName("A"), "${this.javaClass.`package`.name}.I"))

        val aName = curruptName (classTestName("A"))
        val iName = curruptName ("${this.javaClass.`package`.name}.I")

        val carpenterSchema = amqpSchema.carpenterSchema()

        /* whilst there are two unknown classes within the envelope A depends on I so we can't construct a
           schema for A until we have for I */
        assertEquals (1, carpenterSchema.size)
        assertNotEquals (null, carpenterSchema.carpenterSchemas.find { it.name == iName })

        /* since we can't build A it should list I as a dependency*/
        assert (aName in carpenterSchema.dependencies)
        assertEquals (1, carpenterSchema.dependencies[aName]!!.second.size)
        assertEquals (iName, carpenterSchema.dependencies[aName]!!.second[0])

        /* and conversly I should have A listed as a dependent */
        assert(iName in carpenterSchema.dependsOn)
        assertEquals(1, carpenterSchema.dependsOn[iName]!!.size)
        assertEquals(aName, carpenterSchema.dependsOn[iName]!![0])

        val mc = MetaCarpenter (carpenterSchema)

        mc.build()

        assertEquals (0, mc.schemas.carpenterSchemas.size)
        assertEquals (0, mc.schemas.dependencies.size)
        assertEquals (0, mc.schemas.dependsOn.size)
        assertEquals (2, mc.objects.size)
        assert (aName in mc.objects)
        assert (iName in mc.objects)

        mc.objects[aName]!!.constructors[0].newInstance(testI)
    }

    @Test
    fun twoInterfacesAndImplementation() {
        val testI = 69
        val testII = 96

        class A(override val i: Int, override val ii: Int) : I, II

        val a = A(testI, testII)

        val obj = DeserializationInput(factory).deserializeAndReturnEnvelope(serialise(a))

        val amqpSchema = obj.envelope.schema.curruptName(listOf(
                classTestName("A"),
                "${this.javaClass.`package`.name}.I",
                "${this.javaClass.`package`.name}.II"))

        val aName  = curruptName (classTestName("A"))
        val iName  = curruptName ("${this.javaClass.`package`.name}.I")
        val iiName = curruptName ("${this.javaClass.`package`.name}.II")

        val carpenterSchema = amqpSchema.carpenterSchema()

        /* there is nothing preventing us from carpenting up the two interfaces so
           our initial list should contain both interface with A being dependent on both
           and each having A as a dependent */
        assertEquals (2, carpenterSchema.carpenterSchemas.size)
        assertNotNull (carpenterSchema.carpenterSchemas.find { it.name == iName })
        assertNotNull (carpenterSchema.carpenterSchemas.find { it.name == iiName })
        assertNull (carpenterSchema.carpenterSchemas.find { it.name == aName })

        assert (iName in carpenterSchema.dependsOn)
        assertEquals (1, carpenterSchema.dependsOn[iName]?.size)
        assertNotNull (carpenterSchema.dependsOn[iName]?.find ( { it == aName }))

        assert (iiName in carpenterSchema.dependsOn)
        assertEquals (1, carpenterSchema.dependsOn[iiName]?.size)
        assertNotNull (carpenterSchema.dependsOn[iiName]?.find { it == aName })

        assert (aName in carpenterSchema.dependencies)
        assertEquals (2, carpenterSchema.dependencies[aName]!!.second.size)
        assertNotNull (carpenterSchema.dependencies[aName]!!.second.find { it == iName })
        assertNotNull (carpenterSchema.dependencies[aName]!!.second.find { it == iiName })

        val mc = MetaCarpenter (carpenterSchema)

        mc.build()
        assertEquals (0, mc.schemas.carpenterSchemas.size)
        assertEquals (0, mc.schemas.dependencies.size)
        assertEquals (0, mc.schemas.dependsOn.size)
        assertEquals (3, mc.objects.size)
        assert (aName in mc.objects)
        assert (iName in mc.objects)
        assert (iiName in mc.objects)
    }

    @Test
    fun nestedInterfacesAndImplementation() {
        val testI   = 7
        val testIII = 11

        class A(override val i: Int, override val iii : Int) : III

        val a = A(testI, testIII)

        val obj = DeserializationInput(factory).deserializeAndReturnEnvelope(serialise(a))

        val amqpSchema = obj.envelope.schema.curruptName(listOf(
                classTestName("A"),
                "${this.javaClass.`package`.name}.I",
                "${this.javaClass.`package`.name}.III"))

        val aName  = curruptName (classTestName("A"))
        val iName  = curruptName ("${this.javaClass.`package`.name}.I")
        val iiiName = curruptName ("${this.javaClass.`package`.name}.III")

        val carpenterSchema = amqpSchema.carpenterSchema()

        /* Since A depends on III and III extends I we will have to construct them
         * in that reverse order (I -> III -> A) */
        assertEquals (1, carpenterSchema.carpenterSchemas.size)
        assertNotNull (carpenterSchema.carpenterSchemas.find { it.name == iName })
        assertNull (carpenterSchema.carpenterSchemas.find { it.name == iiiName })
        assertNull (carpenterSchema.carpenterSchemas.find { it.name == aName })

        /* I has III as a direct dependent and A as an indirect one */
        assert (iName in carpenterSchema.dependsOn)
        assertEquals (2, carpenterSchema.dependsOn[iName]?.size)
        assertNotNull (carpenterSchema.dependsOn[iName]?.find ( { it == iiiName }))
        assertNotNull (carpenterSchema.dependsOn[iName]?.find ( { it == aName }))

        /* III has A as a dependent */
        assert (iiiName in carpenterSchema.dependsOn)
        assertEquals (1, carpenterSchema.dependsOn[iiiName]?.size)
        assertNotNull (carpenterSchema.dependsOn[iiiName]?.find { it == aName })

        /* converly III depends on I */
        assert (iiiName in carpenterSchema.dependencies)
        assertEquals (1, carpenterSchema.dependencies[iiiName]!!.second.size)
        assertNotNull (carpenterSchema.dependencies[iiiName]!!.second.find { it == iName })

        /* and A depends on III and I*/
        assert (aName in carpenterSchema.dependencies)
        assertEquals (2, carpenterSchema.dependencies[aName]!!.second.size)
        assertNotNull (carpenterSchema.dependencies[aName]!!.second.find { it == iiiName })
        assertNotNull (carpenterSchema.dependencies[aName]!!.second.find { it == iName })

        val mc = MetaCarpenter (carpenterSchema)

        mc.build()
        assertEquals (0, mc.schemas.carpenterSchemas.size)
        assertEquals (0, mc.schemas.dependencies.size)
        assertEquals (0, mc.schemas.dependsOn.size)
        assertEquals (3, mc.objects.size)
        assert (aName in mc.objects)
        assert (iName in mc.objects)
        assert (iiiName in mc.objects)
    }
}


