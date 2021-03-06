/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf;

import static org.gearvrf.utility.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gearvrf.GVRSceneObject;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import android.util.Log;

/**
 * Base class for defining light sources.
 *
 * Lights are implemented by the fragment shader. Each different light
 * implementation corresponds to a subclass of GVRLightBase which is
 * responsible for supplying the shader source code for the light. GearVRF
 * aggregates all of the light source implementations into a single fragment
 * shader.
 *
 * Each subclass of GVRLightBase is a different light implementation and has
 * different shader source. The uniform descriptor is a string which gives the
 * name and type of all uniforms expected in the shader source. It is supplied
 * when a light is created to describe the expected shader input.
 * 
 * {@link GVRShaderTemplate GVRRenderData.bindShader }
 */
public class GVRLightBase extends GVRComponent implements GVRDrawFrameListener
{
    public GVRLightBase(GVRContext gvrContext, GVRSceneObject parent)
    {
        super(gvrContext, NativeLight.ctor(), parent);
        if (parent != null)
        {
            NativeLight.setParent(getNative(), parent.getNative());
        }
        uniformDescriptor = "float enabled float3 world_position float3 world_direction";
        setFloat("enabled", 1.0f);
        setVec3("world_position", 0.0f, 0.0f, 0.0f);
        setVec3("world_direction", 0.0f, 0.0f, 1.0f);
    }

    public GVRLightBase(GVRContext gvrContext)
    {
        super(gvrContext, NativeLight.ctor());
        uniformDescriptor = "float enabled float3 world_position float3 world_direction";
        setFloat("enabled", 1.0f);
        setVec3("world_position", 0.0f, 0.0f, 0.0f);
        setVec3("world_direction", 0.0f, 0.0f, 1.0f);
    }

    public void setOwnerObject(GVRSceneObject newOwner)
    {
        if (owner == newOwner) { return; }
        if (newOwner != null)
        {
            if (owner == null)
            {
                getGVRContext().registerDrawFrameListener(this);
            }
            NativeLight.setParent(getNative(), newOwner.getNative());
        }
        else
        {
            if (owner != null)
            {
                getGVRContext().unregisterDrawFrameListener(this);
            }
            NativeLight.setParent(getNative(), 0);
        }
        super.setOwnerObject(newOwner);
    }

    /**
     * Enable the light.
     */
    public void enable()
    {
        setFloat("enabled", 1.0f);
        NativeLight.enable(getNative());
    }

    /**
     * Disable the light.
     */
    public void disable()
    {
        setFloat("enabled", 0.0f);
        NativeLight.disable(getNative());
    }
    
    /**
     * Get the position of the light in world coordinates.
     * 
     * The position is computed from the scene object the light is attached to.
     * It corresponds to the "world_position" uniform for the light.
     * 
     * @return the world position of the light as a 3 element array
     */
    public float[] getPosition() {
        return getVec3("world_position");
    }

    /**
     * Set the world position of the light.
     * 
     * The position is computed from the scene object the light is attached to.
     * It corresponds to the "world_position" uniform for the light.
     * 
     * @param x
     *            x-coordinate in world coordinate system
     * @param y
     *            y-coordinate in world coordinate system
     * @param z
     *            z-coordinate in world coordinate system
     */
    public void setPosition(float x, float y, float z) {
        setVec3("world_position", x, y, z);
    }

    /**
     * Get the light ID.
     * 
     * This is a string that uniquely identifies the light and is generated by
     * GearVRF when it is added to the scene. It is used to generate fragment
     * shader code.
     */
    public String getLightID()
    {
        return NativeLight.getLightID(getNative());
    }

    /**
     * Access the shader source code implementing this light.
     *
     * The shader code for each light defines a function which computes the
     * color contributed by this light. It takes a structure of uniforms and a
     * <Surface> structure as input and outputs a <Radiance> structure. The
     * format of the uniforms is defined by the shader descriptor. The fragment
     * shader is responsible for computing the surface color and integrating the
     * contribution of each light to the final fragment color. It defines the
     * format of the <Radiance> and <Surface> structures.
     * {@link GVRShaderTemplate } {@link getShaderDescriptor}
     * 
     * @return string with source for light shader
     */
    public String getShaderSource()
    {
        return shaderSource;
    }

    /**
     * Access the descriptor defining the shader uniforms used by this light.
     *
     * Defines the GLSL structure representing the uniform data passed to this
     * light. These must match the structure defined in the shader source code.
     * Each light object maintains a copy of these values and sends them to the
     * shader when they are updated. the format of the <Radiance> and
     * <Surface> structures. {@link GVRShaderTemplate } {@link getShaderSource}
     * 
     * @return String describing light shader uniforms
     */
    public String getUniformDescriptor()
    {
        return uniformDescriptor;
    }

    /**
     * Gets the value of a floating uniform based on its name.
     * 
     * @param key
     *            name of uniform to get
     * @return floating point value of uniform
     * @throws exception
     *             if uniform name not found
     */
    @SuppressWarnings("unused")
    public float getFloat(String key)
    {
        return NativeLight.getFloat(getNative(), key);
    }

    /**
     * Sets the value of a floating uniform based on its name.
     * 
     * @param key
     *            name of uniform to get
     * @param new
     *            floating point value of uniform
     * @throws exception
     *             if uniform name not found
     */
    @SuppressWarnings("unused")
    public void setFloat(String key, float value)
    {
        checkStringNotNullOrEmpty("key", key);
        checkFloatNotNaNOrInfinity("value", value);
        NativeLight.setFloat(getNative(), key, value);
    }

    /**
     * Gets the value of a vec3 floating uniform based on its name.
     * 
     * @param key
     *            name of uniform to get
     * @return vec3 value of uniform
     * @throws exception
     *             if uniform name not found
     */
    public float[] getVec3(String key)
    {
        return NativeLight.getVec3(getNative(), key);
    }

    /**
     * Sets the value of a vec3 uniform based on its name.
     * 
     * @param key
     *            name of uniform to get
     * @param new
     *            vec3 value of uniform
     * @throws exception
     *             if uniform name not found
     */
    public void setVec3(String key, float x, float y, float z)
    {
        checkStringNotNullOrEmpty("key", key);
        NativeLight.setVec3(getNative(), key, x, y, z);
    }

    /**
     * Gets the value of a vec4 floating uniform based on its name.
     * 
     * @param key
     *            name of uniform to get
     * @return vec4 value of uniform
     * @throws exception
     *             if uniform name not found
     */
    public float[] getVec4(String key)
    {
        return NativeLight.getVec4(getNative(), key);
    }

    /**
     * Sets the value of a vec4 uniform based on its name.
     * 
     * @param key
     *            name of uniform to get
     * @param new
     *            vec4 value of uniform
     * @throws exception
     *             if uniform name not found
     */
    public void setVec4(String key, float x, float y, float z, float w)
    {
        checkStringNotNullOrEmpty("key", key);
        NativeLight.setVec4(getNative(), key, x, y, z, w);
    }

    /**
     * Get the GLSL structure describing this light. The name of the structure
     * is derived from the light class name. It is assumed that each different
     * type of light will be represented by a different Java class.
     *
     * @return String with shader structure description.
     */
    public String getShaderStruct()
    {
        Pattern pattern = Pattern.compile("[ ]*([fFiI][loatn]+)([0-9]*)[ ]+([A-Za-z0-9_]+)[,;:]*");
        Matcher matcher = pattern.matcher(uniformDescriptor);
        String structDesc = "struct Struct" + getClass().getSimpleName() + " {\n";
        while (matcher.find())
        {
            String name = matcher.group(3);
            String size = matcher.group(2);
            String type = matcher.group(1);

            if (size.length() > 0)
            {
                if (type.toLowerCase().startsWith("i"))
                    structDesc += "   ivec" + size;
                else
                    structDesc += "   vec" + size;
            }
            else
                structDesc += type;
            structDesc += " " + name + ";\n";
        }
        structDesc += "};\n";
        return structDesc;
    }

    /**
     * Define the shader source to compute the illumination from this light.
     * 
     * This source must be defined at the time the light is constructed.
     * Typically, different subclasses of GVRLightBase will have different
     * illumination functions.
     * 
     * @param source
     *            String with shader source code.
     */
    protected void setShaderSource(String source)
    {
        shaderSource = source;
    }

    /**
     * Get the default orientation of the light when there is no transformation
     * applied.
     */
    public Quaternionf getDefaultOrientation()
    {
        return defaultDir;
    }

    /**
     * Set the default orientation of the light when there is no transformation
     * applied.
     * 
     * GearVRF lights default to looking down the positive Z axis with a light
     * direction of (0, 0, 1). This function lets you change the initial forward
     * vector for lights. This orientation is multiplied by the world
     * transformation matrix of the scene object the light is attached to in
     * order to derive the light direction in world space that is passed to the
     * fragment shader.
     * 
     * @param orientation
     *            quaternion with the initial light orientation
     */
    public void setDefaultOrientation(Quaternionf orientation)
    {
        defaultDir = orientation;
    }

    /**
     * Updates the position and direction of this light from the transform of
     * scene object that owns it.
     */
    public void onDrawFrame(float frameTime)
    {
        if ((getFloat("enabled") <= 0.0f) || (owner == null)) { return; }
        float[] odir = getVec3("world_direction");
        float[] opos = getVec3("world_position");
        GVRSceneObject parent = owner;
        Matrix4f worldmtx = parent.getTransform().getModelMatrix4f();
        Matrix4f lightrot = new Matrix4f();
        Vector3f olddir = new Vector3f(odir[0], odir[1], odir[2]);
        Vector3f oldpos = new Vector3f(opos[0], opos[1], opos[2]);
        Vector3f newdir = new Vector3f(0.0f, 0.0f, 1.0f);
        Vector3f newpos = new Vector3f();

        defaultDir.get(lightrot);
        worldmtx.getTranslation(newpos);
//        lightrot.mul(worldmtx, worldmtx);
        worldmtx.mul(lightrot);
        worldmtx.transformDirection(newdir);
        if ((olddir.x != newdir.x) || (olddir.y != newdir.y) || (olddir.z != newdir.z))
        {
            setVec3("world_direction", newdir.x, newdir.y, newdir.z);
        }
        if ((oldpos.x != newpos.x) || (oldpos.y != newpos.y) || (oldpos.z != newpos.z))
        {
            setVec3("world_position", newpos.x, newpos.y, newpos.z);
        }
    }

    protected Quaternionf defaultDir = new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f);
    protected String shaderSource = null;
    protected String uniformDescriptor = null;
}

class NativeLight
{
    static native long ctor();

    static native void enable(long light);

    static native void disable(long light);

    static native float getFloat(long light, String key);

    static native void setFloat(long light, String key, float value);

    static native float[] getVec3(long light, String key);

    static native void setVec3(long light, String key, float x, float y, float z);

    static native float[] getVec4(long light, String key);

    static native void setVec4(long light, String key, float x, float y, float z, float w);

    static native void setParent(long light, long sceneobj);

    static native String getLightID(long light);

    static native void setLightID(long light, String id);
}
