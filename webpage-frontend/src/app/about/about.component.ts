import {Component} from "@angular/core";
import {environment} from "../../environments/environment";

@Component({
  selector: "app-about",
  template: `
    <div class="container mx-auto my-2">
      <div class="p-4 border rounded">
        <h1 class="text-2xl font-bold mb-4">About TUManyBenchmarks</h1>
        <div class="mb-2">
          <h2 class="text-xl font-bold">Goal</h2>
          <p class="text-gray-700 text-justify">
            TUManyBenchmarks is a service dedicated to providing information about low-level performance metrics
            for different types of virtual machines.
            Our mission is to deliver accurate and reliable benchmark data to help users make informed
            decisions and achieve cost-efficiency.
          </p>
        </div>

        <div class="mb-2">
          <h2 class="text-xl font-bold">Motivation</h2>
          <p class="text-gray-700 text-justify">
            The lack of low-level performance metrics delivered by cloud providers and
            the difficulty of proper benchmarking leaded us to the conclusion that there is a need
            for a new benchmarking service.
            We believe such a service should characterize two attributes: extensibility and transparency.
            Extensibility means that every user should be able to upload their benchmark.
            This feature allow us for simultaneously collecting multiple metrics from many instances
            without unnecessary work and, as a result, allow using multiple metrics for comparison.
            The second characteristic is transparency.
            Transparency means everyone can check what and how benchmarks are executed to
            provide trustworthiness in the presented results.
            Moreover, the second aspect of transparency is achieving transparency of instances' performance
            within and between vendors by publishing their low-level performance metrics.
          </p>
        </div>

        <div class="mb-2">
          <h2 class="text-xl font-bold">Results</h2>
          <p class="text-gray-700 text-justify">
            Our service executes the benchmarks periodically.
            You can access these results using our service by using simple filters or a query console.
            Query console is implemented using <a class="underline" href="https://duckdb.org/docs/api/wasm/overview">DuckDB Wasm</a>.
            DuckDB is an in-process OLAP database.
            Thanks to DuckDB Wasm, the database operates entirely in your browser after downloading data.
            For this reason, you can freely manipulate data, including adding new tables and columns.
            The initial database contains 3 tables:
          </p>
          <ul class="pr-4 text-gray-700 list-disc list-inside">
            <li>instances - containing all essential information about instances, their prices, and detailed benchmark results</li>
            <li>benchmarks - containing all details about benchmarks, such as name and series</li>
            <li>statistics - containing precomputed most commonly used benchmark statistics, such as median</li>
          </ul>
        </div>

        <div>
          <h2 class="text-xl font-bold">Contribute</h2>
          <p class="text-gray-700">
            If you want to add a new benchmark create a Pull Request to our repository:
            <a [href]="environment.repositoryUrl">{{ environment.repositoryUrl }}</a>
            <br/>
            To upload new benchmark, you have to provide:
          </p>
            <ul class="pr-4 text-gray-700 list-disc list-inside">
              <li>benchmark files - the program or script that will be executed by service</li>
              <li>configuration.yml - file specifying how to execute benchmark</li>
              <li>Ansible file - to prepare the benchmark environment and download dependencies</li>
              <li>Script formating benchmark output to JSON</li>
            </ul>
        </div>
      </div>
    </div>
  `
})
export class AboutComponent {
  protected readonly environment = environment;
}
