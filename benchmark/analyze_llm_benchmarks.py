from pathlib import Path
import os
import re
import textwrap
from datetime import date
import webbrowser

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns


BASE_DIR = Path(__file__).parent / "llm"
OUT_DIR = BASE_DIR / "analysis_output"
OUT_DIR.mkdir(exist_ok=True)

THINKING_HUE_ORDER = ["Non-thinking", "Thinking"]
THINKING_PALETTE = {
    "Non-thinking": "#4C72B0",
    "Thinking": "#DD8452",
}


def safe_select_columns(df: pd.DataFrame, columns: list[str]) -> pd.DataFrame:
    """Select only columns that exist in the dataframe."""
    existing_columns = [col for col in columns if col in df.columns]
    return df[existing_columns] if existing_columns else df.iloc[:, :0]


def benchmark_name_from_path(path: Path) -> str:
    return path.stem


def display_benchmark_name(benchmark_name: str) -> str:
    clean = benchmark_name
    if clean.startswith("llm_"):
        clean = clean[4:]
    if clean.endswith("_benchmark"):
        clean = clean[:-10]
    return clean[:1].upper() + clean[1:]


def load_data(path: Path) -> pd.DataFrame:
    df = pd.read_csv(path, sep=";")
    if "Success Rate" in df.columns:
        df["Success Rate Num"] = df["Success Rate"].str.rstrip("%").astype(float)
    if "Thinking" in df.columns:
        df["Thinking Bool"] = df["Thinking"].astype(str).str.lower().eq("true")
    if "Model" in df.columns:
        df["Model Family"] = df["Model"].apply(extract_model_family)
    if "Provider" in df.columns:
        df["Deployment Type"] = df["Provider"].apply(classify_deployment_type)
    return df


def extract_model_family(model_name: str) -> str:
    candidate = str(model_name).strip().lower()
    if "/" in candidate:
        candidate = candidate.split("/")[-1]
    family = re.split(r"[:\-]", candidate, maxsplit=1)[0]
    versioned_family_match = re.match(r"^([a-z]+)\d+(?:\.\d+)?$", family)
    if versioned_family_match:
        return versioned_family_match.group(1)
    return family or candidate


def display_model_family_name(family_name: str) -> str:
    candidate = str(family_name).strip().lower()
    special_cases = {
        "gpt": "GPT",
        "lfm": "LFM",
    }
    if candidate in special_cases:
        return special_cases[candidate]
    return re.sub(r"[_\-.]+", " ", candidate).title()


def wrap_chart_label(label: str, width: int = 20) -> str:
    raw_label = str(label)
    if len(raw_label) <= width:
        return raw_label

    normalized_label = re.sub(r"([/:])", r"\1 ", raw_label)
    wrapped_lines = textwrap.wrap(normalized_label, width=width, break_long_words=False, break_on_hyphens=True)
    return "\n".join(line.replace("/ ", "/\n").replace(": ", ":\n") for line in wrapped_lines)


def extract_model_size_billions(model_name: str) -> float | None:
    candidate = str(model_name).strip().lower()
    if "/" in candidate:
        candidate = candidate.split("/")[-1]

    # Prefer explicit size tags like ':24b', ':350m', or compact variants such as ':e2b'.
    explicit_matches = re.findall(r":\s*(?:[a-z]+)?\s*(\d+(?:\.\d+)?)\s*([bm])\b", candidate)
    if explicit_matches:
        value, unit = explicit_matches[-1]
        size = float(value)
        return size if unit == "b" else size / 1000.0

    # Fallback for names such as 'gemma-4-31b-it', 'phi4-mini:3.8b', or 'gemma4-e4b'.
    generic_matches = re.findall(r"(?:^|[^a-z0-9])(?:[a-z]+)?(\d+(?:\.\d+)?)\s*([bm])\b", candidate)
    if generic_matches:
        value, unit = generic_matches[-1]
        size = float(value)
        return size if unit == "b" else size / 1000.0

    return None


def classify_deployment_type(provider_name: str) -> str:
    candidate = str(provider_name).strip().lower()
    if "local" in candidate or "ollama" in candidate:
        return "Local"
    return "Cloud"


def to_markdown_with_right_aligned_columns(df: pd.DataFrame, right_aligned_columns: set[str]) -> str:
    table = df.to_markdown(index=False)
    lines = table.splitlines()
    if len(lines) < 2:
        return table

    headers = [cell.strip() for cell in lines[0].split("|")[1:-1]]
    separators = [cell for cell in lines[1].split("|")[1:-1]]

    updated_separators = []
    for header, separator in zip(headers, separators):
        content_width = max(3, len(separator.strip().strip(":")))
        if header in right_aligned_columns:
            updated_separators.append(" " + ("-" * (content_width - 1)) + ":")
        else:
            updated_separators.append(separator)

    lines[1] = "|" + "|".join(updated_separators) + "|"
    return "\n".join(lines)


def create_plots(df: pd.DataFrame, out_dir: Path) -> None:
    sns.set_theme(style="whitegrid")

    if "Success Rate Num" not in df.columns or "Avg Time" not in df.columns or "Model" not in df.columns:
        return  # Skip plotting if required columns are missing

    all_perfect = df[df["Success Rate Num"] == 100].sort_values("Avg Time")
    model_order = all_perfect.sort_values("Avg Time")["Model"].drop_duplicates().tolist()
    plt.figure(figsize=(12, max(6, len(all_perfect) * 0.35)))
    if "Thinking" in df.columns:
        sns.barplot(data=all_perfect, x="Avg Time", y="Model", hue="Thinking", dodge=True, order=model_order)
    else:
        sns.barplot(data=all_perfect, x="Avg Time", y="Model", order=model_order)
    plt.title("All models with 100% success rate (sorted by shortest runtime)")
    plt.xlabel("Average time")
    plt.ylabel("Model")
    if "Thinking" in df.columns:
        plt.legend(title="Thinking", loc="lower right")
    plt.tight_layout()
    plt.savefig(out_dir / "all_100_success_models_by_time.png", dpi=150)
    plt.close()

    plt.figure(figsize=(10, 6))
    if "Thinking" in df.columns:
        sns.scatterplot(data=df, x="Avg Time", y="Success Rate Num", hue="Thinking", style="Thinking", s=90)
    else:
        sns.scatterplot(data=df, x="Avg Time", y="Success Rate Num", s=90)
    plt.title("Trade-off: runtime vs. success rate")
    plt.xlabel("Average time")
    plt.ylabel("Success Rate (%)")
    plt.gca().invert_yaxis()
    plt.tight_layout()
    plt.savefig(out_dir / "success_vs_time_scatter.png", dpi=150)
    plt.close()

    if "Thinking Bool" in df.columns and "Model" in df.columns:
        models_with_thinking = set(df.loc[df["Thinking Bool"], "Model"])
        thinking_time_df = df[df["Model"].isin(models_with_thinking)]

        if not thinking_time_df.empty and "Thinking" in thinking_time_df.columns:
            plt.figure(figsize=(8, 6))
            sns.boxplot(data=thinking_time_df, x="Thinking", y="Avg Time")
            plt.title("Effect of Thinking on runtime (models with Thinking support only)")
            plt.xlabel("Thinking")
            plt.ylabel("Average time")
            plt.tight_layout()
            plt.savefig(out_dir / "thinking_vs_time_boxplot.png", dpi=150)
            plt.close()

    if "Model Family" in df.columns:
        family_perf = (
            df.groupby("Model Family")
            .agg(avg_success=("Success Rate Num", "mean"), avg_time=("Avg Time", "mean"), runs=("Model", "count"))
            .sort_values(["avg_success", "avg_time"], ascending=[False, True])
            .head(12)
            .reset_index()
        )
        family_perf["Model Family Display"] = family_perf["Model Family"].apply(display_model_family_name)
        plt.figure(figsize=(11, 6))
        sns.scatterplot(data=family_perf, x="avg_time", y="avg_success", size="runs", hue="Model Family Display", sizes=(80, 500))
        plt.title("Model families: average success rate vs. runtime")
        plt.xlabel("Average time")
        plt.ylabel("Average success rate (%)")
        plt.gca().invert_yaxis()
        plt.legend(loc="center left", bbox_to_anchor=(1.02, 0.5), borderaxespad=0)
        plt.tight_layout()
        plt.savefig(out_dir / "family_performance_scatter.png", dpi=150)
        plt.close()


def build_summary(df: pd.DataFrame, benchmark_name: str) -> str:
    right_aligned_columns = {"Thinking", "Thinking Bool", "Success Rate"}

    runs = len(df)
    unique_models = df["Model"].nunique() if "Model" in df.columns else 0
    mean_success = df["Success Rate Num"].mean() if "Success Rate Num" in df.columns else 0
    median_success = df["Success Rate Num"].median() if "Success Rate Num" in df.columns else 0
    mean_time = df["Avg Time"].mean() if "Avg Time" in df.columns else 0

    all_models = (
        df.sort_values(["Success Rate Num", "Avg Time"], ascending=[False, True])
    )
    all_models = safe_select_columns(all_models, ["Model", "Thinking", "Success Rate", "Avg Time"]).reset_index(drop=True)

    models_with_thinking = set(df.loc[df["Thinking Bool"], "Model"]) if "Thinking Bool" in df.columns and "Model" in df.columns else set()
    thinking_comparison_df = df[df["Model"].isin(models_with_thinking)] if models_with_thinking and "Model" in df.columns else df.iloc[0:0]

    by_thinking = pd.DataFrame()
    if "Thinking Bool" in df.columns and not thinking_comparison_df.empty:
        by_thinking = (
            thinking_comparison_df.groupby("Thinking Bool")
            .agg(avg_success=("Success Rate Num", "mean") if "Success Rate Num" in thinking_comparison_df.columns else (lambda x: 0), 
                 avg_time=("Avg Time", "mean") if "Avg Time" in thinking_comparison_df.columns else (lambda x: 0), 
                 runs=("Model", "count") if "Model" in thinking_comparison_df.columns else (lambda x: 0))
            .reset_index()
        )

    by_family = pd.DataFrame()
    if "Model Family" in df.columns:
        by_family = (
            df.groupby("Model Family")
            .agg(avg_success=("Success Rate Num", "mean") if "Success Rate Num" in df.columns else (lambda x: 0), 
                 avg_time=("Avg Time", "mean") if "Avg Time" in df.columns else (lambda x: 0), 
                 runs=("Model", "count") if "Model" in df.columns else (lambda x: 0))
            .sort_values(["avg_success", "avg_time"], ascending=[False, True])
            .head(10)
            .reset_index()
        )
        by_family["Model Family"] = by_family["Model Family"].apply(display_model_family_name)
        by_family["avg_success"] = by_family["avg_success"].round(1)
        by_family["avg_time"] = by_family["avg_time"].round(2)

    lines = []
    lines.append(f"# Analysis: {benchmark_name}")
    lines.append("")
    lines.append("## Overview")
    lines.append(f"- Number of runs: {runs}")
    lines.append(f"- Number of models (unique): {unique_models}")
    lines.append(f"- Mean success rate: {mean_success:.1f}%")
    lines.append(f"- Median Success Rate: {median_success:.1f}%")
    lines.append(f"- Mean runtime (Avg Time): {mean_time:.2f}")
    lines.append("")

    lines.append("## All models (sorted by success rate, then runtime)")
    lines.append(to_markdown_with_right_aligned_columns(all_models, right_aligned_columns))
    lines.append("")

    lines.append("## Thinking vs. non-thinking comparison (models with Thinking support only)")
    lines.append(to_markdown_with_right_aligned_columns(by_thinking, right_aligned_columns))
    lines.append("")

    lines.append("## Top 10 model families (by average quality)")
    lines.append(to_markdown_with_right_aligned_columns(by_family, right_aligned_columns))
    lines.append("")

    lines.append("## Generated plots")
    lines.append("")
    lines.append("### All 100% success models")
    lines.append("![All models with 100% success rate by runtime](all_100_success_models_by_time.png)")
    lines.append("")
    lines.append("### Success vs Time")
    lines.append("![Trade-off between runtime and success rate](success_vs_time_scatter.png)")
    lines.append("")
    lines.append("### Thinking vs Time")
    lines.append("![Effect of Thinking on runtime](thinking_vs_time_boxplot.png)")
    lines.append("")
    lines.append("### Family Performance")
    lines.append("![Model family comparison](family_performance_scatter.png)")

    return "\n".join(lines)


def build_top_sections(benchmark_frames: dict[str, pd.DataFrame]) -> list[str]:
    sections = []
    for benchmark_name, df in sorted(benchmark_frames.items()):
        benchmark_display = display_benchmark_name(benchmark_name)
        top5 = (
            df.sort_values(["Success Rate Num", "Avg Time"], ascending=[False, True])
        )
        top5 = safe_select_columns(top5, ["Model", "Thinking", "Success Rate", "Avg Time"]).head(5).reset_index(drop=True)
        sections.append(f"### {benchmark_display}")
        sections.append("")
        for rank, (_, row) in enumerate(top5.iterrows(), start=1):
            sections.append(
                f"{rank}. {row['Model'] if 'Model' in row.index else 'N/A'} (Thinking: {str(row.get('Thinking', 'N/A')).lower()}, {row.get('Success Rate', 'N/A')}, {row.get('Avg Time', 'N/A')})"
            )
        sections.append("")
    return sections


def create_overall_family_model_size_bar_charts(benchmark_frames: dict[str, pd.DataFrame], out_dir: Path) -> list[tuple[str, str, str | None, str | None]]:
    chart_rows = []
    for benchmark_name, df in benchmark_frames.items():
        if not {"Model", "Success Rate Num", "Model Family"}.issubset(df.columns):
            continue

        benchmark_display = display_benchmark_name(benchmark_name)
        temp_df = df[["Model", "Success Rate Num", "Model Family"]].copy()
        if "Avg Tokens" in df.columns:
            temp_df["Avg Tokens"] = pd.to_numeric(df["Avg Tokens"], errors="coerce")
        else:
            temp_df["Avg Tokens"] = pd.NA
        if "Avg Time" in df.columns:
            temp_df["Avg Time"] = pd.to_numeric(df["Avg Time"], errors="coerce")
        else:
            temp_df["Avg Time"] = pd.NA
        if "Thinking Bool" in df.columns:
            temp_df["Thinking Bool"] = df["Thinking Bool"]
        elif "Thinking" in df.columns:
            temp_df["Thinking Bool"] = df["Thinking"].astype(str).str.lower().eq("true")
        else:
            temp_df["Thinking Bool"] = False

        temp_df["Benchmark"] = benchmark_display
        temp_df["Model Size (B)"] = temp_df["Model"].apply(extract_model_size_billions)
        temp_df = temp_df.dropna(subset=["Model Size (B)"])
        if temp_df.empty:
            continue
        chart_rows.append(temp_df)

    if not chart_rows:
        return []

    combined_df = pd.concat(chart_rows, ignore_index=True)
    combined_df["Thinking Category"] = combined_df["Thinking Bool"].map({True: "Thinking", False: "Non-thinking"})

    family_chart_dir = out_dir / "family_model_size_charts"
    family_chart_dir.mkdir(exist_ok=True)

    generated_charts: list[tuple[str, str, str | None, str | None]] = []
    family_order = sorted(combined_df["Model Family"].dropna().unique())
    for family in family_order:
        family_df = combined_df[combined_df["Model Family"] == family].copy()
        if family_df.empty:
            continue

        family_plot_df = (
            family_df.groupby(["Model", "Thinking Category"], as_index=False)
            .agg(
                success_rate=("Success Rate Num", "mean"),
                avg_tokens=("Avg Tokens", "mean"),
                avg_time=("Avg Time", "mean"),
                model_size_billions=("Model Size (B)", "mean"),
                runs=("Benchmark", "count"),
                benchmarks=("Benchmark", "nunique"),
            )
            .sort_values(["model_size_billions", "Model", "Thinking Category"])
        )
        if family_plot_df.empty:
            continue

        model_order = family_plot_df.sort_values(["model_size_billions", "Model"])["Model"].drop_duplicates().tolist()
        family_plot_df["Model Label"] = family_plot_df["Model"].apply(wrap_chart_label)
        model_label_order = [wrap_chart_label(model_name) for model_name in model_order]

        family_slug = re.sub(r"[^a-z0-9]+", "_", str(family).lower()).strip("_") or "unknown"
        chart_name = f"{family_slug}_success_vs_size_by_thinking_overall.png"
        chart_path = family_chart_dir / chart_name

        plt.figure(figsize=(10, 5))
        sns.barplot(
            data=family_plot_df,
            x="Model Label",
            y="success_rate",
            hue="Thinking Category",
            dodge=True,
            order=model_label_order,
            hue_order=THINKING_HUE_ORDER,
            palette=THINKING_PALETTE,
        )
        family_display = display_model_family_name(family)
        plt.title(f"{family_display}: success rate by model and thinking mode (all benchmarks)")
        plt.xlabel("Model")
        plt.ylabel("Success rate (%)")
        plt.ylim(0, 100)
        plt.legend(title="Mode")
        plt.tight_layout()
        plt.savefig(chart_path, dpi=150)
        plt.close()

        token_chart_rel_path: str | None = None
        token_plot_df = family_plot_df.dropna(subset=["avg_tokens"]).copy()
        token_plot_df = token_plot_df[token_plot_df["avg_tokens"] > 0]
        if not token_plot_df.empty:
            token_chart_name = f"{family_slug}_avg_tokens_vs_size_by_thinking_overall.png"
            token_chart_path = family_chart_dir / token_chart_name

            plt.figure(figsize=(10, 5))
            sns.barplot(
                data=token_plot_df,
                x="Model Label",
                y="avg_tokens",
                hue="Thinking Category",
                dodge=True,
                order=model_label_order,
                hue_order=THINKING_HUE_ORDER,
                palette=THINKING_PALETTE,
            )
            plt.title(f"{family_display}: average token count by model (ordered by model size)")
            plt.xlabel("Model")
            plt.ylabel("Average token count")
            plt.yscale("log")
            plt.legend(title="Mode")
            plt.tight_layout()
            plt.savefig(token_chart_path, dpi=150)
            plt.close()
            token_chart_rel_path = f"family_model_size_charts/{token_chart_name}"

        time_chart_rel_path: str | None = None
        time_plot_df = family_plot_df.dropna(subset=["avg_time"]).copy()
        time_plot_df = time_plot_df[time_plot_df["avg_time"] >= 0]
        if not time_plot_df.empty:
            time_chart_name = f"{family_slug}_avg_time_vs_size_by_thinking_overall.png"
            time_chart_path = family_chart_dir / time_chart_name

            plt.figure(figsize=(10, 5))
            sns.barplot(
                data=time_plot_df,
                x="Model Label",
                y="avg_time",
                hue="Thinking Category",
                dodge=True,
                order=model_label_order,
                hue_order=THINKING_HUE_ORDER,
                palette=THINKING_PALETTE,
            )
            plt.title(f"{family_display}: average runtime by model (ordered by model size)")
            plt.xlabel("Model")
            plt.ylabel("Average runtime")
            plt.legend(title="Mode")
            plt.tight_layout()
            plt.savefig(time_chart_path, dpi=150)
            plt.close()
            time_chart_rel_path = f"family_model_size_charts/{time_chart_name}"

        generated_charts.append(
            (
                family_display,
                f"family_model_size_charts/{chart_name}",
                token_chart_rel_path,
                time_chart_rel_path,
            )
        )

    return generated_charts


def build_deployment_summary(benchmark_frames: dict[str, pd.DataFrame], deployment_type: str) -> str:
    filtered_frames = {
        benchmark_name: df[df["Deployment Type"] == deployment_type].copy()
        for benchmark_name, df in sorted(benchmark_frames.items())
        if "Deployment Type" in df.columns
    }
    filtered_frames = {benchmark_name: df for benchmark_name, df in filtered_frames.items() if not df.empty}

    lines = []
    lines.append(f"# {deployment_type} analysis across all benchmarks")
    lines.append("")
    lines.append(f"As of: {date.today().isoformat()}")
    lines.append("")

    if not filtered_frames:
        lines.append(f"No runs found with deployment type {deployment_type}.")
        return "\n".join(lines)

    overview_rows = []
    thinking_rows = []
    model_benchmark_rows = []
    benchmark_display_names = [display_benchmark_name(name) for name in sorted(filtered_frames)]

    for benchmark_name, df in filtered_frames.items():
        benchmark_display = display_benchmark_name(benchmark_name)
        success = df["Success Rate Num"] if "Success Rate Num" in df.columns else pd.Series([])
        thinking_df = df[df["Thinking Bool"]] if "Thinking Bool" in df.columns else df.iloc[0:0]
        non_thinking_df = df[~df["Thinking Bool"]] if "Thinking Bool" in df.columns else df

        model_agg = pd.DataFrame()
        if "Model" in df.columns:
            agg_cols = {"success_rate": ("Success Rate Num", "mean") if "Success Rate Num" in df.columns else (lambda x: 0)}
            agg_cols["avg_time"] = ("Avg Time", "mean") if "Avg Time" in df.columns else (lambda x: 0)
            if "Thinking Bool" in df.columns:
                model_agg = (
                    df.groupby(["Model", "Thinking Bool"])
                    .agg(**agg_cols)
                    .reset_index()
                )
            else:
                model_agg = (
                    df.groupby(["Model"])
                    .agg(**agg_cols)
                    .reset_index()
                )

        for _, row in model_agg.iterrows():
            model_benchmark_rows.append(
                {
                    "Model": row["Model"],
                    "Thinking": "true" if bool(row.get("Thinking Bool", False)) else "false",
                    "Benchmark": benchmark_display,
                    "Success Rate": round(float(row.get("success_rate", 0)), 1),
                    "Avg Time": round(float(row.get("avg_time", 0)), 2),
                }
            )

        overview_rows.append(
            {
                "Benchmark": benchmark_display,
                "Runs": int(len(df)),
                "Unique Models": int(df["Model"].nunique()) if "Model" in df.columns else 0,
                "Mean Success Rate": f"{success.mean():.1f}%" if not success.empty else "n/a",
                "Median Success Rate": f"{success.median():.1f}%" if not success.empty else "n/a",
                "Mean Runtime": round(float(df["Avg Time"].mean()), 2) if "Avg Time" in df.columns else "n/a",
                "Share 100%": f"{(success.eq(100).mean() * 100):.1f}%" if not success.empty else "n/a",
                "Share 0%": f"{(success.eq(0).mean() * 100):.1f}%" if not success.empty else "n/a",
            }
        )

        thinking_rows.append(
            {
                "Benchmark": benchmark_display,
                "Think Runs": int(len(thinking_df)),
                "Non-Think Runs": int(len(non_thinking_df)),
                "Think Mean Success": f"{thinking_df['Success Rate Num'].mean():.1f}%" if not thinking_df.empty and "Success Rate Num" in thinking_df.columns else "n/a",
                "Non-Think Mean Success": f"{non_thinking_df['Success Rate Num'].mean():.1f}%"
                if not non_thinking_df.empty and "Success Rate Num" in non_thinking_df.columns
                else "n/a",
                "Think Mean Time": round(float(thinking_df["Avg Time"].mean()), 2) if not thinking_df.empty and "Avg Time" in thinking_df.columns else "n/a",
                "Non-Think Mean Time": round(float(non_thinking_df["Avg Time"].mean()), 2)
                if not non_thinking_df.empty and "Avg Time" in non_thinking_df.columns
                else "n/a",
            }
        )

    overview_df = pd.DataFrame(overview_rows)
    thinking_df = pd.DataFrame(thinking_rows)
    model_benchmark_df = pd.DataFrame(model_benchmark_rows)

    score_pivot = model_benchmark_df.pivot(index=["Model", "Thinking"], columns="Benchmark", values="Success Rate") if not model_benchmark_df.empty else pd.DataFrame()
    time_pivot = model_benchmark_df.pivot(index=["Model", "Thinking"], columns="Benchmark", values="Avg Time") if not model_benchmark_df.empty else pd.DataFrame()
    coverage = score_pivot.notna().sum(axis=1).rename("Benchmarks") if not score_pivot.empty else pd.Series([])
    avg_success = score_pivot.mean(axis=1, skipna=True).rename("Avg Success") if not score_pivot.empty else pd.Series([])

    model_compare_df = pd.DataFrame(index=score_pivot.index) if not score_pivot.empty else pd.DataFrame()
    if not model_compare_df.empty:
        model_compare_df["Benchmarks"] = coverage
        model_compare_df["Avg Success"] = avg_success

        for benchmark_display in benchmark_display_names:
            model_compare_df[f"{benchmark_display} Success %"] = score_pivot[benchmark_display].map(
                lambda v: f"{v:.1f}%" if pd.notna(v) else "-"
            )
            model_compare_df[f"{benchmark_display} Avg Time"] = time_pivot[benchmark_display].map(
                lambda v: f"{v:.2f}" if pd.notna(v) else "-"
            )

        model_compare_df = model_compare_df.reset_index()
        model_compare_df = model_compare_df.sort_values(
            ["Benchmarks", "Avg Success", "Model", "Thinking"],
            ascending=[False, False, True, True],
        )
        model_compare_df["Avg Success"] = model_compare_df["Avg Success"].map(lambda v: f"{v:.1f}%")

    lines.append(
        f"This analysis contains only runs with deployment type {deployment_type}. It is based on all *_benchmark.csv files."
    )
    lines.append("")
    lines.append("## 1) At-a-glance comparison")
    lines.append("")
    lines.append(overview_df.to_markdown(index=False))
    lines.append("")
    lines.append("## 2) Thinking vs. Non-Thinking")
    lines.append("")
    lines.append(thinking_df.to_markdown(index=False))
    lines.append("")
    lines.append("## 3) Models: success rate and runtime per benchmark (split by Thinking)")
    lines.append("")
    lines.append(model_compare_df.to_markdown(index=False))
    lines.append("")
    lines.append("## 4) Best performers per benchmark (top by success, then time)")
    lines.append("")
    lines.extend(build_top_sections(filtered_frames))

    return "\n".join(lines)


def build_overall_summary(benchmark_frames: dict[str, pd.DataFrame], overall_family_charts: list[tuple[str, str, str | None, str | None]]) -> str:
    overview_rows = []
    thinking_rows = []
    model_benchmark_rows = []
    benchmark_display_names = [display_benchmark_name(name) for name in sorted(benchmark_frames)]

    for benchmark_name, df in sorted(benchmark_frames.items()):
        benchmark_display = display_benchmark_name(benchmark_name)
        success = df["Success Rate Num"] if "Success Rate Num" in df.columns else pd.Series([])
        thinking_df = df[df["Thinking Bool"]] if "Thinking Bool" in df.columns else df.iloc[0:0]
        non_thinking_df = df[~df["Thinking Bool"]] if "Thinking Bool" in df.columns else df

        model_agg = pd.DataFrame()
        if "Model" in df.columns:
            agg_cols = {}
            if "Success Rate Num" in df.columns:
                agg_cols["success_rate"] = ("Success Rate Num", "mean")
            if "Avg Time" in df.columns:
                agg_cols["avg_time"] = ("Avg Time", "mean")
                
            groupby_cols = ["Model", "Thinking Bool", "Deployment Type"]
            existing_groupby_cols = [col for col in groupby_cols if col in df.columns]
            if existing_groupby_cols and agg_cols:
                model_agg = (
                    df.groupby(existing_groupby_cols)
                    .agg(**agg_cols)
                    .reset_index()
                )
        
        for _, row in model_agg.iterrows():
            model_benchmark_rows.append(
                {
                    "Model": row.get("Model", "N/A"),
                    "Thinking": "true" if bool(row.get("Thinking Bool", False)) else "false",
                    "Deployment Type": row.get("Deployment Type", "N/A"),
                    "Benchmark": benchmark_display,
                    "Success Rate": round(float(row.get("success_rate", 0)), 1) if "success_rate" in row.index else 0,
                    "Avg Time": round(float(row.get("avg_time", 0)), 2) if "avg_time" in row.index else 0,
                }
            )

        overview_rows.append(
            {
                "Benchmark": benchmark_display,
                "Runs": int(len(df)),
                "Unique Models": int(df["Model"].nunique()) if "Model" in df.columns else 0,
                "Mean Success Rate": f"{success.mean():.1f}%" if not success.empty else "n/a",
                "Median Success Rate": f"{success.median():.1f}%" if not success.empty else "n/a",
                "Mean Runtime": round(float(df["Avg Time"].mean()), 2) if "Avg Time" in df.columns else "n/a",
                "Share 100%": f"{(success.eq(100).mean() * 100):.1f}%" if not success.empty else "n/a",
                "Share 0%": f"{(success.eq(0).mean() * 100):.1f}%" if not success.empty else "n/a",
            }
        )

        thinking_rows.append(
            {
                "Benchmark": benchmark_display,
                "Think Runs": int(len(thinking_df)),
                "Non-Think Runs": int(len(non_thinking_df)),
                "Think Mean Success": f"{thinking_df['Success Rate Num'].mean():.1f}%" if not thinking_df.empty and "Success Rate Num" in thinking_df.columns else "n/a",
                "Non-Think Mean Success": f"{non_thinking_df['Success Rate Num'].mean():.1f}%"
                if not non_thinking_df.empty and "Success Rate Num" in non_thinking_df.columns
                else "n/a",
                "Think Mean Time": round(float(thinking_df["Avg Time"].mean()), 2) if not thinking_df.empty and "Avg Time" in thinking_df.columns else "n/a",
                "Non-Think Mean Time": round(float(non_thinking_df["Avg Time"].mean()), 2)
                if not non_thinking_df.empty and "Avg Time" in non_thinking_df.columns
                else "n/a",
            }
        )

    overview_df = pd.DataFrame(overview_rows)
    thinking_df = pd.DataFrame(thinking_rows)
    model_benchmark_df = pd.DataFrame(model_benchmark_rows)

    score_pivot = model_benchmark_df.pivot(index=["Model", "Thinking"], columns="Benchmark", values="Success Rate") if not model_benchmark_df.empty else pd.DataFrame()
    time_pivot = model_benchmark_df.pivot(index=["Model", "Thinking"], columns="Benchmark", values="Avg Time") if not model_benchmark_df.empty else pd.DataFrame()
    coverage = score_pivot.notna().sum(axis=1).rename("Benchmarks") if not score_pivot.empty else pd.Series([])
    avg_success = score_pivot.mean(axis=1, skipna=True).rename("Avg Success") if not score_pivot.empty else pd.Series([])

    model_compare_df = pd.DataFrame(index=score_pivot.index) if not score_pivot.empty else pd.DataFrame()
    if not model_compare_df.empty:
        model_compare_df["Benchmarks"] = coverage
        model_compare_df["Avg Success"] = avg_success

        for benchmark_display in benchmark_display_names:
            if benchmark_display in score_pivot.columns:
                model_compare_df[f"{benchmark_display} Success %"] = score_pivot[benchmark_display].map(
                    lambda v: f"{v:.1f}%" if pd.notna(v) else "-"
                )
            if benchmark_display in time_pivot.columns:
                model_compare_df[f"{benchmark_display} Avg Time"] = time_pivot[benchmark_display].map(
                    lambda v: f"{v:.2f}" if pd.notna(v) else "-"
                )

        model_compare_df = model_compare_df.reset_index()
        model_compare_df = model_compare_df.sort_values(
            ["Benchmarks", "Avg Success", "Model", "Thinking"],
            ascending=[False, False, True, True],
        )
        model_compare_df["Avg Success"] = model_compare_df["Avg Success"].map(lambda v: f"{v:.1f}%")

    model_size_scatter_df = model_benchmark_df.copy() if not model_benchmark_df.empty else pd.DataFrame()
    if not model_size_scatter_df.empty and "Model" in model_size_scatter_df.columns:
        model_size_scatter_df["Model Size (B)"] = model_size_scatter_df["Model"].apply(extract_model_size_billions)
        model_size_scatter_df = model_size_scatter_df.dropna(subset=["Model Size (B)"])

    scatter_path = OUT_DIR / "model_size_vs_success_scatter_by_benchmark.png"
    if not model_size_scatter_df.empty and "Success Rate" in model_size_scatter_df.columns and "Benchmark" in model_size_scatter_df.columns:
        plt.figure(figsize=(11, 7))
        sns.scatterplot(
            data=model_size_scatter_df,
            x="Model Size (B)",
            y="Success Rate",
            hue="Benchmark",
            style="Benchmark",
            s=90,
            alpha=0.85,
        )
        plt.title("Model size vs. success rate (color-coded by benchmark)")
        plt.xlabel("Model size (parameters in billions)")
        plt.ylabel("Success Rate (%)")
        plt.tight_layout()
        plt.savefig(scatter_path, dpi=150)
        plt.close()

    time_success_scatter_path = OUT_DIR / "time_vs_success_scatter_by_benchmark.png"
    if not model_benchmark_df.empty and "Avg Time" in model_benchmark_df.columns and "Success Rate" in model_benchmark_df.columns and "Benchmark" in model_benchmark_df.columns:
        plt.figure(figsize=(11, 7))
        sns.scatterplot(
            data=model_benchmark_df,
            x="Avg Time",
            y="Success Rate",
            hue="Benchmark",
            style="Benchmark",
            s=90,
            alpha=0.85,
        )
        plt.title("Runtime vs. success rate (color- and marker-coded by benchmark)")
        plt.xlabel("Average time")
        plt.ylabel("Success Rate (%)")
        plt.tight_layout()
        plt.savefig(time_success_scatter_path, dpi=150)
        plt.close()

    deployment_summary_df = pd.DataFrame()
    if not model_benchmark_df.empty and "Deployment Type" in model_benchmark_df.columns:
        agg_cols = {}
        if "Model" in model_benchmark_df.columns:
            agg_cols["Runs"] = ("Model", "count")
            agg_cols["Unique_Models"] = ("Model", "nunique")
        if "Success Rate" in model_benchmark_df.columns:
            agg_cols["Avg_Success"] = ("Success Rate", "mean")
        if "Avg Time" in model_benchmark_df.columns:
            agg_cols["Avg_Time"] = ("Avg Time", "mean")
            
        if agg_cols:
            deployment_summary_df = (
                model_benchmark_df.groupby("Deployment Type")
                .agg(**agg_cols)
                .reset_index()
                .rename(columns={"Unique_Models": "Unique Models", "Avg_Success": "Avg Success", "Avg_Time": "Avg Time"})
            )
        if not deployment_summary_df.empty:
            if "Avg Success" in deployment_summary_df.columns:
                deployment_summary_df["Avg Success"] = deployment_summary_df["Avg Success"].map(lambda v: f"{v:.1f}%")
            if "Avg Time" in deployment_summary_df.columns:
                deployment_summary_df["Avg Time"] = deployment_summary_df["Avg Time"].map(lambda v: f"{v:.2f}")

    lines = []
    lines.append("# Overall analysis across all benchmarks")
    lines.append("")
    lines.append(f"As of: {date.today().isoformat()}")
    lines.append("")
    lines.append("This analysis is generated automatically from all *_benchmark.csv files.")
    lines.append("")
    lines.append("## 1) At-a-glance comparison")
    lines.append("")
    lines.append(overview_df.to_markdown(index=False))
    lines.append("")
    lines.append("## 2) Thinking vs. Non-Thinking")
    lines.append("")
    lines.append(thinking_df.to_markdown(index=False))
    lines.append("")
    lines.append("## 3) Best performers per benchmark (top by success, then time)")
    lines.append("")
    lines.extend(build_top_sections(benchmark_frames))
    if not model_size_scatter_df.empty:
        lines.append("## 4) Model size vs. success rate")
        lines.append("")
        lines.append("![Model size vs. success rate by benchmark](model_size_vs_success_scatter_by_benchmark.png)")
        lines.append("")
        lines.append(
            f"Data points with inferable model size used: {len(model_size_scatter_df)} of {len(model_benchmark_df)}"
        )
        lines.append("")
    if not model_benchmark_df.empty:
        lines.append("## 5) Runtime vs. success rate")
        lines.append("")
        lines.append(
            "![Runtime vs. success rate by benchmark](time_vs_success_scatter_by_benchmark.png)"
        )
        lines.append("")
        lines.append("Colors and markers encode benchmarks.")
        lines.append("")

    if overall_family_charts:
        lines.append("## 6) Family model-size charts (accumulated over benchmarks)")
        lines.append("")
        lines.append("Each chart aggregates all benchmarks for one model family and compares Thinking vs non-thinking bars.")
        lines.append("")
        for family_name, success_chart_rel_path, token_chart_rel_path, time_chart_rel_path in overall_family_charts:
            lines.append(f"### {family_name}")
            lines.append(f"![{family_name}: success rate vs model size, accumulated over benchmarks]({success_chart_rel_path})")
            if token_chart_rel_path:
                lines.append("")
                lines.append(
                    f"![{family_name}: average token count vs model size, accumulated over benchmarks]({token_chart_rel_path})"
                )
            if time_chart_rel_path:
                lines.append("")
                lines.append(
                    f"![{family_name}: average runtime vs model size, accumulated over benchmarks]({time_chart_rel_path})"
                )
            lines.append("")

    lines.append("## 7) Links to individual analyses")
    lines.append("")
    lines.append("- analysis_output/cloud_analysis.md")
    lines.append("- analysis_output/local_analysis.md")
    for benchmark_name in sorted(benchmark_frames):
        lines.append(f"- analysis_output/{benchmark_name}/analysis.md")

    return "\n".join(lines)


def open_generated_report(report_path: Path) -> None:
    """Open the generated report file with the default system handler."""
    if not report_path.exists():
        print(f"Report not found: {report_path}")
        return

    try:
        if os.name == "nt":
            os.startfile(str(report_path))  # type: ignore[attr-defined]
        else:
            webbrowser.open(report_path.resolve().as_uri())
    except OSError as exc:
        print(f"Could not open report automatically: {exc}")


def analyze_benchmark(csv_path: Path) -> None:
    benchmark_name = benchmark_name_from_path(csv_path)
    benchmark_out_dir = OUT_DIR / benchmark_name
    benchmark_out_dir.mkdir(parents=True, exist_ok=True)
    old_score_plot = benchmark_out_dir / "score_ranking_models.png"
    if old_score_plot.exists():
        old_score_plot.unlink()

    df = load_data(csv_path)
    create_plots(df, benchmark_out_dir)
    summary = build_summary(df, benchmark_name)
    (benchmark_out_dir / "analysis.md").write_text(summary, encoding="utf-8")


def analyze_all_benchmarks(benchmark_files: list[Path]) -> None:
    benchmark_frames: dict[str, pd.DataFrame] = {}
    for csv_path in benchmark_files:
        benchmark_name = benchmark_name_from_path(csv_path)
        analyze_benchmark(csv_path)
        benchmark_frames[benchmark_name] = load_data(csv_path)

    overall_family_charts = create_overall_family_model_size_bar_charts(benchmark_frames, OUT_DIR)
    overall_summary = build_overall_summary(benchmark_frames, overall_family_charts)
    (OUT_DIR / "overall_analysis.md").write_text(overall_summary, encoding="utf-8")
    cloud_summary = build_deployment_summary(benchmark_frames, "Cloud")
    (OUT_DIR / "cloud_analysis.md").write_text(cloud_summary, encoding="utf-8")
    local_summary = build_deployment_summary(benchmark_frames, "Local")
    (OUT_DIR / "local_analysis.md").write_text(local_summary, encoding="utf-8")


def main() -> None:
    benchmark_files = sorted(BASE_DIR.glob("*_benchmark.csv"))
    if not benchmark_files:
        print("No benchmark CSV found (*_benchmark.csv).")
        return

    analyze_all_benchmarks(benchmark_files)
    open_generated_report(OUT_DIR / "overall_analysis.md")

    print("Analysis complete. Files in:", OUT_DIR)


if __name__ == "__main__":
    main()