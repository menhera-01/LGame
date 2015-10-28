/**
 * Copyright 2008 - 2015 The Loon Game Engine Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.5
 */
package loon;

import java.util.Random;

import loon.event.Drawable;
import loon.event.Updateable;
import loon.geom.Dimension;
import loon.opengl.GLEx;
import loon.opengl.ShaderProgram;

public class LSystem {

	public static String FONT_NAME = "Dialog";

	public static String ENCODING = "UTF-8";

	public static String APP_NAME = "Loon";

	public static float EMULATOR_BUTTIN_SCALE = 1f;

	public static final int DEFAULT_MAX_CACHE_SIZE = 32;

	public static final float scaleWidth = 1f;

	public static final float scaleHeight = 1f;

	public static final Dimension viewSize = new Dimension(480, 320);

	public static boolean PAUSED = false;

	public static boolean AUTO_REPAINT = true;

	public static boolean USE_LOG = true;

	public static boolean LOCK_SCREEN = false;
	// 包内默认的图片路径
	final static public String FRAMEWORK_IMG_NAME = "loon_";

	// 行分隔符
	final static public String LS = System.getProperty("line.separator", "\n");

	// 文件分割符
	final static public String FS = System.getProperty("file.separator", "\\");

	public static boolean isHTML5() {
		if (_base == null) {
			return false;
		}
		return _base.type() == LGame.Type.HTML5;
	}

	public static String getFileName(String name) {
		if (name == null) {
			return "";
		}
		if (name.indexOf(LSystem.FS) != -1) {
			int length = name.length();
			int size = name.lastIndexOf(LSystem.FS) + 1;
			if (size < length) {
				return name.substring(size, length);
			} else {
				return "";
			}
		} else {
			int length = name.length();
			int size = name.lastIndexOf('/') + 1;
			if (size < length) {
				return name.substring(size, length);
			} else {
				return "";
			}
		}
	}

	public static String getExtension(String name) {
		if (name == null) {
			return "";
		}
		int index = name.lastIndexOf(".");
		if (index == -1) {
			return "";
		} else {
			return name.substring(index + 1);
		}
	}

	public static void mainBeginDraw() {
		if (_base == null) {
			return;
		}
		Display game = _base.display();
		if (game != null) {
			GLEx gl = game.GL();
			if (!gl.running()) {
				gl.saveTx();
				gl.begin();
			}
		}
	}

	public static void mainEndDraw() {
		if (_base == null) {
			return;
		}
		Display game = _base.display();
		if (game != null) {
			GLEx gl = game.GL();
			if (gl.running()) {
				gl.restore();
				gl.end();
				gl.freeBatchBuffer();
			}
		}
	}

	public final static void close(LRelease rel) {
		if (rel != null) {
			try {
				rel.close();
				rel = null;
			} catch (Exception e) {
			}
		}
	}

	public final static LProcess getProcess() {
		return _process;
	}

	public final static void load(Updateable u) {
		if (_process != null) {
			_process.addLoad(u);
		}
	}

	public final static void unload(Updateable u) {
		if (_process != null) {
			_process.addUnLoad(u);
		}
	}

	public final static void clearUpdate() {
		if (_process != null) {
			_process.removeAllDrawing();
		}
	}

	public final static void drawing(Drawable d) {
		if (_process != null) {
			_process.addDrawing(d);
		}
	}

	public final static void clearDrawing() {
		if (_process != null) {
			_process.removeAllDrawing();
		}
	}

	public static final int VERTEX_SIZE = 2 + 1 + 2;
	public static final int SPRITE_SIZE = 4 * VERTEX_SIZE;

	static public String createVertexShader(boolean hasNormals,
			boolean hasColors, int numTexCoords) {
		String shader = "attribute vec4 "
				+ ShaderProgram.POSITION_ATTRIBUTE
				+ ";\n"
				+ (hasNormals ? "attribute vec3 "
						+ ShaderProgram.NORMAL_ATTRIBUTE + ";\n" : "")
				+ (hasColors ? "attribute vec4 "
						+ ShaderProgram.COLOR_ATTRIBUTE + ";\n" : "");

		for (int i = 0; i < numTexCoords; i++) {
			shader += "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + i
					+ ";\n";
		}

		shader += "uniform mat4 u_projModelView;\n";
		shader += (hasColors ? "varying vec4 v_col;\n" : "");

		for (int i = 0; i < numTexCoords; i++) {
			shader += "varying vec2 v_tex" + i + ";\n";
		}

		shader += "void main() {\n"
				+ "   gl_Position = u_projModelView * "
				+ ShaderProgram.POSITION_ATTRIBUTE
				+ ";\n"
				+ (hasColors ? "   v_col = " + ShaderProgram.COLOR_ATTRIBUTE
						+ ";\n" : "");

		for (int i = 0; i < numTexCoords; i++) {
			shader += "   v_tex" + i + " = " + ShaderProgram.TEXCOORD_ATTRIBUTE
					+ i + ";\n";
		}
		shader += "   gl_PointSize = 1.0;\n";
		shader += "}\n";
		return shader;
	}

	static public String createFragmentShader(boolean hasNormals,
			boolean hasColors, int numTexCoords) {
		String shader = "#ifdef GL_ES\n" + "precision mediump float;\n"
				+ "#endif\n";

		if (hasColors)
			shader += "varying vec4 v_col;\n";
		for (int i = 0; i < numTexCoords; i++) {
			shader += "varying vec2 v_tex" + i + ";\n";
			shader += "uniform sampler2D u_sampler" + i + ";\n";
		}

		shader += "void main() {\n" + "   gl_FragColor = "
				+ (hasColors ? "v_col" : "vec4(1, 1, 1, 1)");

		if (numTexCoords > 0)
			shader += " * ";

		for (int i = 0; i < numTexCoords; i++) {
			if (i == numTexCoords - 1) {
				shader += " texture2D(u_sampler" + i + ",  v_tex" + i + ")";
			} else {
				shader += " texture2D(u_sampler" + i + ",  v_tex" + i + ") *";
			}
		}

		shader += ";\n}";
		return shader;
	}

	static public ShaderProgram createDefaultShader() {
		String vertexShader = "attribute vec4 "
				+ ShaderProgram.POSITION_ATTRIBUTE
				+ ";\n" //
				+ "attribute vec4 "
				+ ShaderProgram.COLOR_ATTRIBUTE
				+ ";\n" //
				+ "attribute vec2 "
				+ ShaderProgram.TEXCOORD_ATTRIBUTE
				+ "0;\n" //
				+ "uniform mat4 u_projTrans;\n" //
				+ "varying vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = "
				+ ShaderProgram.COLOR_ATTRIBUTE
				+ ";\n" //
				+ "   v_color.a = v_color.a * (255.0/254.0);\n" //
				+ "   v_texCoords = "
				+ ShaderProgram.TEXCOORD_ATTRIBUTE
				+ "0;\n" //
				+ "   gl_Position =  u_projTrans * "
				+ ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
				+ "}\n";
		String fragmentShader = "#ifdef GL_ES\n" //
				+ "#define LOWP lowp\n" //
				+ "precision mediump float;\n" //
				+ "#else\n" //
				+ "#define LOWP \n" //
				+ "#endif\n" //
				+ "varying LOWP vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "uniform sampler2D u_texture;\n" //
				+ "void main()\n"//
				+ "{\n" //
				+ "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" //
				+ "}";

		ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
		if (shader.isCompiled() == false)
			throw new IllegalArgumentException("Error compiling shader: "
					+ shader.getLog());
		return shader;
	}

	static public String createGlobalVertexShader(boolean hasNormals,
			boolean hasColors, int numTexCoords) {
		String shader = "attribute vec4 "
				+ ShaderProgram.POSITION_ATTRIBUTE
				+ ";\n"
				+ (hasNormals ? "attribute vec3 "
						+ ShaderProgram.NORMAL_ATTRIBUTE + ";\n" : "")
				+ (hasColors ? "attribute vec4 "
						+ ShaderProgram.COLOR_ATTRIBUTE + ";\n" : "");

		for (int i = 0; i < numTexCoords; i++) {
			shader += "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + i
					+ ";\n";
		}

		shader += "uniform mat4 u_projModelView;\n";
		shader += (hasColors ? "uniform vec4 v_col;\n" : "");

		for (int i = 0; i < numTexCoords; i++) {
			shader += "varying vec2 v_tex" + i + ";\n";
		}

		shader += "void main() {\n"
				+ "   gl_Position = u_projModelView * "
				+ ShaderProgram.POSITION_ATTRIBUTE
				+ ";\n"
				+ (hasColors ? "   v_col = " + ShaderProgram.COLOR_ATTRIBUTE
						+ ";\n" : "");

		for (int i = 0; i < numTexCoords; i++) {
			shader += "   v_tex" + i + " = " + ShaderProgram.TEXCOORD_ATTRIBUTE
					+ i + ";\n";
		}
		shader += "   gl_PointSize = 1.0;\n";
		shader += "}\n";
		return shader;
	}

	static public String createGlobalFragmentShader(boolean hasNormals,
			boolean hasColors, int numTexCoords) {
		String shader = "#ifdef GL_ES\n" + "precision mediump float;\n"
				+ "#endif\n";
		if (hasColors) {
			shader += "uniform vec4 v_col;\n";
		}
		for (int i = 0; i < numTexCoords; i++) {
			shader += "varying vec2 v_tex" + i + ";\n";
			shader += "uniform sampler2D u_sampler" + i + ";\n";
		}
		shader += "void main() {\n" + "   gl_FragColor = "
				+ (hasColors ? "v_col" : "vec4(1, 1, 1, 1)");
		if (numTexCoords > 0) {
			shader += " * ";
		}
		for (int i = 0; i < numTexCoords; i++) {
			if (i == numTexCoords - 1) {
				shader += " texture2D(u_sampler" + i + ",  v_tex" + i + ")";
			} else {
				shader += " texture2D(u_sampler" + i + ",  v_tex" + i + ") *";
			}
		}
		shader += ";\n}";
		return shader;
	}

	static public ShaderProgram createGlobalShader() {
		String vertexShader = "attribute vec4 "
				+ ShaderProgram.POSITION_ATTRIBUTE
				+ ";\n" //
				+ "attribute vec4 "
				+ ShaderProgram.COLOR_ATTRIBUTE
				+ ";\n" //
				+ "attribute vec2 "
				+ ShaderProgram.TEXCOORD_ATTRIBUTE
				+ "0;\n" //
				+ "uniform mat4 u_projTrans;\n" //
				+ "varying vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = "
				+ ShaderProgram.COLOR_ATTRIBUTE
				+ ";\n" //
				+ "   v_color.a = v_color.a * (255.0/254.0);\n" //
				+ "   v_texCoords = "
				+ ShaderProgram.TEXCOORD_ATTRIBUTE
				+ "0;\n" //
				+ "   gl_Position =  u_projTrans * "
				+ ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
				+ "}\n";
		String fragmentShader = "#ifdef GL_ES\n" //
				+ "#define LOWP lowp\n" //
				+ "precision mediump float;\n" //
				+ "#else\n" //
				+ "#define LOWP \n" //
				+ "#endif\n" //
				+ "uniform LOWP vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "uniform sampler2D u_texture;\n" //
				+ "void main()\n"//
				+ "{\n" //
				+ "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" //
				+ "}";

		ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
		if (shader.isCompiled() == false) {
			throw new IllegalArgumentException("Error compiling shader: "
					+ shader.getLog());
		}
		return shader;
	}

	final static public Random random = new Random();

	public final static String version = "0.5";

	public static int MODE_NORMAL = 1;

	public static int MODE_ALPHA_MAP = 2;

	public static int MODE_ALPHA_BLEND = 3;

	public static int MODE_COLOR_MULTIPLY = 4;

	public static int MODE_ADD = 5;

	public static int MODE_SCREEN = 6;

	public static int MODE_ALPHA = 7;

	public static int MODE_SPEED = 8;

	public static int MODE_ALPHA_ONE = 9;

	public static int MODE_NONE = 10;

	// 秒
	final static public long SECOND = 1000;

	// 分
	final static public long MINUTE = SECOND * 60;

	// 小时
	final static public long HOUR = MINUTE * 60;

	// 天
	final static public long DAY = HOUR * 24;

	// 周
	final static public long WEEK = DAY * 7;

	// 理论上一年
	final static public long YEAR = DAY * 365;

	static LGame _base;

	static LProcess _process;

	public static LGame base() {
		return _base;
	}

	protected static void init(LGame game) {
		_base = game;
		_process = new LProcess(game);
		_base.log().debug("The Loon Game Engine is Begin");
	}

	public static String format(float value) {
		String fmt = String.valueOf(value);
		return fmt.indexOf('.') == -1 ? (fmt + ".0") : fmt;
	}

	public static int unite(int hashCode, boolean value) {
		int v = value ? 1231 : 1237;
		return unite(hashCode, v);
	}

	public static int unite(int hashCode, long value) {
		int v = (int) (value ^ (value >>> 32));
		return unite(hashCode, v);
	}

	public static int unite(int hashCode, float value) {
		int v = Float.floatToIntBits(value);
		return unite(hashCode, v);
	}

	public static int unite(int hashCode, double value) {
		long v = Double.doubleToLongBits(value);
		return unite(hashCode, v);
	}

	public static int unite(int hashCode, Object value) {
		return unite(hashCode, value.hashCode());
	}

	public static int unite(int hashCode, int value) {
		return 31 * hashCode + value;
	}
}
