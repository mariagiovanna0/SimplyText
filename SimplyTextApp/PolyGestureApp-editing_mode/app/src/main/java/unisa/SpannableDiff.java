package unisa;

import java.util.ArrayList;
import java.util.LinkedList;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.text.style.UnderlineSpan;

public class SpannableDiff {
	private static final int[] COLORS = new int[] { 0x7FFF0000, 0x7F008000,
			0x79FF00FF, 0x7F800000, 0x79000080 };

	private interface SpanMarker {
	}

	private static class EndBarSpan extends ReplacementSpan implements
			SpanMarker {
		private int backgroundColor = 0xFFFF0000;

		public EndBarSpan(int backgroundColor) {
			this.backgroundColor = backgroundColor;
		}

		@Override
		public void draw(Canvas canvas, CharSequence text, int start, int end,
				float x, int top, int y, int bottom, Paint paint) {
			float te = x + paint.measureText(text, start, end);
			float len = paint.measureText(" ") * 0.2f;
			RectF rect = new RectF(te - len, top, te + len, bottom);
			int col = paint.getColor();
			paint.setColor(backgroundColor);
			canvas.drawRect(rect, paint);
			paint.setColor(col);
			canvas.drawText(text, start, end, x, y, paint);
		}

		@Override
		public int getSize(Paint paint, CharSequence text, int start, int end,
				Paint.FontMetricsInt fm) {
			return Math.round(paint.measureText(text, start, end));
		}
	}

	private static class MForegroundColorSpan extends ForegroundColorSpan
			implements SpanMarker {
		public MForegroundColorSpan(int color) {
			super(color);
		}
	}

	private static class MUnderlineSpan extends UnderlineSpan implements
			SpanMarker {
	}

	public static Spannable highlightEditDiff(CharSequence source,
			CharSequence target) {
		String srcStr = source.toString();
		LinkedList<Diff> dfs = new diff_match_patch().diff_main(srcStr,
				target.toString());

		SpannableStringBuilder out = new SpannableStringBuilder(srcStr);
		int cLen = 0;
		ArrayList<String> insdel = new ArrayList<String>();
		for (Diff d : dfs) {
			if (d.operation.equals(diff_match_patch.Operation.EQUAL)) {
				cLen += d.text.length();
			} else {
				String cur = d.text.trim();
				int curIdx = insdel.indexOf(d.text.trim());
				if (curIdx == -1) {
					curIdx = insdel.size();
					insdel.add(cur);
				}
				if (d.operation.equals(diff_match_patch.Operation.INSERT)) {
					if (cLen != 0) {
						int last = cLen - 1;
						if (last != 0
								&& Character.isWhitespace(out.charAt(last))
								&& out.charAt(last) == d.text.charAt(d.text
										.length() - 1))
							last--;
						if (last + 1 < out.length() && Character.isWhitespace(out.charAt(last + 1))) {
							out.setSpan(new EndBarSpan(COLORS[curIdx
											% COLORS.length]), last, last + 1,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						} else {
							out.setSpan(new BackgroundColorSpan(COLORS[curIdx
											% COLORS.length]), last, last + 1,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
					}
				} else {
					int l = cLen;
					cLen += d.text.length();
					int end = cLen;
					if (l != 0 && Character.isWhitespace(out.charAt(l - 1))
							&& out.charAt(l - 1) == out.charAt(end - 1)) {
						l--;
						end--;
					}
					out.setSpan(new MForegroundColorSpan(COLORS[curIdx
							% COLORS.length]), l, end,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					for (int i = l; i < end; i++)
						if (Character.isWhitespace(out.charAt(i)))
							out.setSpan(new MUnderlineSpan(), i, i + 1,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
		}
		if (source instanceof Spannable) {
			Spannable or = (Spannable) source;
			for (Object sp : or.getSpans(0, source.length(), Object.class)) {
				if (!(sp instanceof SpanMarker))
					out.setSpan(sp, or.getSpanStart(sp), or.getSpanEnd(sp),
							or.getSpanFlags(sp));
			}
		}
		return out;
	}
}
